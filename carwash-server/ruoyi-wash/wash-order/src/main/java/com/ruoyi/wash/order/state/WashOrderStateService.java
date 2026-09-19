package com.ruoyi.wash.order.state;

import com.ruoyi.wash.common.api.ApiException;
import com.ruoyi.wash.common.api.ErrorCode;
import com.ruoyi.wash.common.statemachine.OrderEvent;
import com.ruoyi.wash.common.statemachine.OrderOperatorType;
import com.ruoyi.wash.common.statemachine.OrderStateMachine;
import com.ruoyi.wash.common.statemachine.OrderStatus;
import com.ruoyi.wash.common.statemachine.OrderStatusLog;
import com.ruoyi.wash.order.domain.WashOrder;
import com.ruoyi.wash.network.service.WashSlotService;
import com.ruoyi.wash.order.event.OrderCanceledEvent;
import com.ruoyi.wash.order.mapper.WashOrderMapper;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneId;
import java.util.EnumSet;
import java.util.Set;

/**
 * 订单状态流转的唯一入口 —— 业务代码只允许调用本服务，禁止直接 update 状态。
 *
 * <p>把 wash-common 里的状态机闸机接入真实环境：
 * 锁 = Redis、状态读写 = MyBatis、副作用 = 写流转日志（只增不删）。
 */
@Service
public class WashOrderStateService {

    private static final Logger log = LoggerFactory.getLogger(WashOrderStateService.class);

    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");
    /** 与 WashOrderCreateService 保持一致，key 规范见技术方案 5.3 */
    private static final String CAPACITY_PREFIX = "capacity:";

    /** 系统取消原因：留痕用，客户能在时间轴里看到是谁、为什么取消 */
    private static final String REASON_PAY_TIMEOUT = "支付超时自动取消";
    private static final String REASON_EMERGENCY = "客户紧急取回钥匙";

    private final OrderRedisLock lock;
    private final WashOrderStatusAccessor accessor;
    private final WashOrderMapper orderMapper;
    private OrderStateMachine stateMachine;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Autowired
    private WashSlotService slotService;

    @Autowired
    public WashOrderStateService(OrderRedisLock lock, WashOrderStatusAccessor accessor, WashOrderMapper orderMapper) {
        this.lock = lock;
        this.accessor = accessor;
        this.orderMapper = orderMapper;
    }

    @PostConstruct
    public void init() {
        this.stateMachine = new OrderStateMachine(lock, accessor);
        this.stateMachine.addListener(this::writeLog);
    }

    /** 支付成功：WAIT_PAY → WAIT_KEY。只校验归属，不校验支付方式（真实支付回调走同一入口）。 */
    public OrderStatus paySuccess(String orderNo, Long memberId) {
        WashOrder order = requireOwned(orderNo, memberId);
        OrderStateMachine.TransitionContext ctx = OrderStateMachine.TransitionContext.builder()
                .orderNo(order.getOrderNo())
                .event(OrderEvent.PAY_SUCCESS)
                .operatorType(OrderOperatorType.CUSTOMER)
                .operatorId(memberId)
                .expectFrom(OrderStatus.WAIT_PAY)
                .build();
        return stateMachine.fire(ctx);
    }

    /**
     * 取消订单：走状态机 CANCEL 事件。
     *
     * <p>客户自助取消仅限车未动的 4 个状态（WAIT_PAY / WAIT_KEY / KEY_IN / PICKING），
     * 其余状态由状态机直接拒绝（B1002），这里不重复判断，避免规则两处漂移。
     *
     * <p>取消后按契约：回补产能 → 释放预占格口 → 已支付的自动触发退款（APPLY_REFUND → REFUNDING）。
     */
    public OrderStatus cancel(String orderNo, Long memberId, String reason) {
        return doCancel(orderNo, memberId, OrderOperatorType.CUSTOMER, reason);
    }

    /**
     * 后台人工取消：与客户端取消的区别是触发方为 ADMIN，
     * 因此不受「客户仅可取消车未动的 4 个状态」限制（如已开洗也能由客服取消）。
     */
    public OrderStatus adminCancel(String orderNo, Long operatorId, String reason) {
        return doCancel(orderNo, operatorId, OrderOperatorType.ADMIN, reason);
    }

    /**
     * 客户提交评价：WAIT_REVIEW → FINISHED。
     *
     * <p>必须走状态机而不是直接改状态：评价是订单主链路的收尾节点，
     * 时间轴与后台追溯都依赖流转日志。
     */
    public OrderStatus submitReview(String orderNo, Long memberId) {
        WashOrder order = requireOwned(orderNo, memberId);
        OrderStateMachine.TransitionContext ctx = OrderStateMachine.TransitionContext.builder()
                .orderNo(order.getOrderNo())
                .event(OrderEvent.REVIEW_SUBMIT)
                .operatorType(OrderOperatorType.CUSTOMER)
                .operatorId(memberId)
                .build();
        return stateMachine.fire(ctx);
    }

    /**
     * 系统取消（支付超时等定时任务触发）：触发方 JOB。
     *
     * <p>副作用必须与客户取消完全一致（回补产能 + 释放格口），否则超时未支付的订单会
     * 永久占着预占格口，柜子很快被占满，新订单直接报 B2003（无空闲格口）。
     */
    public OrderStatus systemCancel(String orderNo) {
        return doCancel(orderNo, null, OrderOperatorType.JOB, REASON_PAY_TIMEOUT);
    }

    /**
     * 紧急取钥匙。契约：openapi.yaml POST /api/v1/orders/{orderNo}/emergency-take-key。
     *
     * <p>车尚未移动（客户可自助取消的 4 个状态）→ 取消订单，已支付的一律全额退款，并告警留痕；
     * 车已取走或已在作业 → 按契约拒绝（B1002）。
     */
    public OrderStatus emergencyTakeKey(String orderNo, Long memberId) {
        WashOrder order = requireOwned(orderNo, memberId);
        OrderStatus from = OrderStatus.of(order.getStatus());
        if (!from.isCustomerCancelable()) {
            throw new ApiException(ErrorCode.B1002, "当前状态不支持紧急取回钥匙，当前状态：" + from.getLabel());
        }
        // 红线：异常场景必须留痕并告警，运营要能看到是谁在什么时候紧急取回
        log.warn("[紧急取钥匙] 告警：orderNo={} memberId={} 触发前状态={}", orderNo, memberId, from.getLabel());
        return doCancel(orderNo, memberId, OrderOperatorType.CUSTOMER, REASON_EMERGENCY);
    }

    private OrderStatus doCancel(String orderNo, Long operatorId, OrderOperatorType operatorType, String reason) {
        if (reason == null || reason.isBlank()) {
            // 契约 required: [reason]，对应错误码 B1004
            throw new ApiException(ErrorCode.B1004);
        }
        WashOrder order = orderMapper.selectByOrderNo(orderNo);
        if (order == null) {
            throw new ApiException(ErrorCode.B1001);
        }
        OrderStatus from = OrderStatus.of(order.getStatus());

        OrderStateMachine.TransitionContext ctx = OrderStateMachine.TransitionContext.builder()
                .orderNo(order.getOrderNo())
                .event(OrderEvent.CANCEL)
                .operatorType(operatorType)
                .operatorId(operatorId)
                .reason(reason)
                .build();
        stateMachine.fire(ctx);

        // 取消原因双写：流转日志（留痕）+ 订单主表（后台可检索）
        orderMapper.updateCancelReason(order.getOrderNo(), reason);

        releaseCapacity(order);
        // 释放预占的格口（取消时车还没动，格口里没钥匙，直接释放即可）
        slotService.release(order.getOrderNo());

        // 未支付（WAIT_PAY）取消无需退款；已支付的一律退全款（已确认决策）。
        // 这里只发事件，真正建退款单、调渠道、推进状态机由 wash-pay 负责（避免订单域依赖支付域）。
        if (from != OrderStatus.WAIT_PAY) {
            eventPublisher.publishEvent(new OrderCanceledEvent(
                    order.getOrderNo(), order.getMemberId(), order.getPayAmount(), reason));
        }
        return OrderStatus.CANCELED;
    }

    /**
     * 后台（客服/站长）允许触发的事件白名单 —— 干预收敛，唯一定义处。
     *
     * <p>不在名单内的一律拒绝，**即便状态机规则允许**。为什么不直接放开：
     * <ul>
     *   <li>PAY_SUCCESS 只能由支付回调触发：后台代付 = 钱没到账却放行订单；</li>
     *   <li>APPLY_REFUND / REFUND_SUCCESS 属支付域，必须由退款单与渠道结果驱动，
     *       后台直接标"退款成功"而钱没退 = 财务事故；</li>
     *   <li>REVIEW_SUBMIT / AUTO_FINISH 是客户与系统的收尾动作，不代表人的操作；</li>
     *   <li>CANCEL 有独立的后台取消接口（带原因与留痕），不混在推进里。</li>
     * </ul>
     */
    private static final Set<OrderEvent> ADMIN_ALLOWED_EVENTS = EnumSet.of(
            OrderEvent.DEPOSIT_KEY, OrderEvent.TAKE_KEY, OrderEvent.PICK_CAR_DONE,
            OrderEvent.ARRIVE_STATION, OrderEvent.SOP_DONE, OrderEvent.QC_FAIL,
            OrderEvent.QC_PASS, OrderEvent.LEAVE_STATION, OrderEvent.RETURN_DONE,
            OrderEvent.TAKE_KEY_BACK, OrderEvent.POSTPONE);

    /** 需要二次确认的高危事件：推进后基本不可撤销（订单收尾 / 打回返工） */
    private static final Set<OrderEvent> ADMIN_CONFIRM_REQUIRED = EnumSet.of(
            OrderEvent.TAKE_KEY_BACK, OrderEvent.QC_FAIL);

    /** 后台是否可触发该事件（供后台"可推进事件"下拉过滤，前端不另写一份名单） */
    public static boolean isAdminAllowed(OrderEvent event) {
        return ADMIN_ALLOWED_EVENTS.contains(event);
    }

    /** 该事件是否需要二次确认 */
    public static boolean isConfirmRequired(OrderEvent event) {
        return ADMIN_CONFIRM_REQUIRED.contains(event);
    }

    /**
     * 后台人工推进：客服/站长在异常场景下（如柜机故障、司机忘打卡）手动触发事件。
     *
     * <p>三重约束：① 必须在白名单内；② 原因必填（留痕）；③ 能否流转仍由状态机规则表决定，
     * 后台只是"换了个触发方"，不能绕过规则（例如不能在 WAIT_PAY 上触发 QC_PASS）。
     */
    public OrderStatus adminFire(String orderNo, OrderEvent event, Long operatorId, String reason) {
        if (!ADMIN_ALLOWED_EVENTS.contains(event)) {
            throw new ApiException(ErrorCode.B1002, "该事件不允许后台触发：" + event.getLabel());
        }
        if (reason == null || reason.isBlank()) {
            throw new ApiException(ErrorCode.A0001, "后台推进必须填写原因");
        }
        WashOrder order = orderMapper.selectByOrderNo(orderNo);
        if (order == null) {
            throw new ApiException(ErrorCode.B1001);
        }
        OrderStateMachine.TransitionContext ctx = OrderStateMachine.TransitionContext.builder()
                .orderNo(order.getOrderNo())
                .event(event)
                .operatorType(OrderOperatorType.ADMIN)
                .operatorId(operatorId)
                .reason(reason)
                .build();
        return stateMachine.fire(ctx);
    }

    /**
     * 系统触发（支付回调、退款结果、定时任务）：触发方为 JOB。
     * 与 adminFire 一样受规则表约束，只是换了个操作者标记，便于日志区分是人还是系统。
     */
    public OrderStatus systemFire(String orderNo, OrderEvent event, String reason) {
        WashOrder order = orderMapper.selectByOrderNo(orderNo);
        if (order == null) {
            throw new ApiException(ErrorCode.B1001);
        }
        OrderStateMachine.TransitionContext ctx = OrderStateMachine.TransitionContext.builder()
                .orderNo(order.getOrderNo())
                .event(event)
                .operatorType(OrderOperatorType.JOB)
                .reason(reason)
                .build();
        return stateMachine.fire(ctx);
    }

    /** 产能回补：下单时 DECR 过，取消要加回去；key 不存在说明当日未限量，无需处理。 */
    private void releaseCapacity(WashOrder order) {
        if (order.getAppointTime() == null) {
            return;
        }
        String date = Instant.ofEpochMilli(order.getAppointTime()).atZone(ZONE).toLocalDate().toString();
        String key = CAPACITY_PREFIX + order.getSiteId() + ":" + date;
        if (Boolean.TRUE.equals(stringRedisTemplate.hasKey(key))) {
            stringRedisTemplate.opsForValue().increment(key);
        }
    }

    private WashOrder requireOwned(String orderNo, Long memberId) {
        WashOrder order = orderMapper.selectByOrderNo(orderNo);
        if (order == null || !order.getMemberId().equals(memberId)) {
            throw new ApiException(ErrorCode.B1001);
        }
        return order;
    }

    /** 副作用：写流转日志。红线的"每一次流转必须留痕"在这里落地。 */
    private void writeLog(OrderStateMachine.TransitionContext ctx, OrderStatus from, OrderStatus to) {
        WashOrder order = orderMapper.selectByOrderNo(ctx.getOrderNo());
        OrderStatusLog log = new OrderStatusLog();
        log.setOrderId(order == null ? null : order.getOrderId());
        log.setFromStatus(from.name());
        log.setToStatus(to.name());
        log.setEvent(ctx.getEvent().name());
        log.setOperatorType(ctx.getOperatorType().name());
        log.setOperatorId(ctx.getOperatorId());
        // source 与 operatorType 同源（规则表第三节决策），避免两套枚举漂移
        log.setSource(ctx.getOperatorType().name());
        log.setReason(ctx.getReason());
        orderMapper.insertStatusLog(log);
    }
}
