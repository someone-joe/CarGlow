package com.ruoyi.wash.order.state;

import com.ruoyi.wash.common.api.ApiException;
import com.ruoyi.wash.common.api.ErrorCode;
import com.ruoyi.wash.common.statemachine.OrderEvent;
import com.ruoyi.wash.common.statemachine.OrderOperatorType;
import com.ruoyi.wash.common.statemachine.OrderStateMachine;
import com.ruoyi.wash.common.statemachine.OrderStatus;
import com.ruoyi.wash.common.statemachine.OrderStatusLog;
import com.ruoyi.wash.order.domain.WashOrder;
import com.ruoyi.wash.order.mapper.WashOrderMapper;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneId;

/**
 * 订单状态流转的唯一入口 —— 业务代码只允许调用本服务，禁止直接 update 状态。
 *
 * <p>把 wash-common 里的状态机闸机接入真实环境：
 * 锁 = Redis、状态读写 = MyBatis、副作用 = 写流转日志（只增不删）。
 */
@Service
public class WashOrderStateService {

    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");
    /** 与 WashOrderCreateService 保持一致，key 规范见技术方案 5.3 */
    private static final String CAPACITY_PREFIX = "capacity:";

    private final OrderRedisLock lock;
    private final WashOrderStatusAccessor accessor;
    private final WashOrderMapper orderMapper;
    private OrderStateMachine stateMachine;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

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
     * <p>取消后按契约：回补产能 → 已支付的自动触发退款（APPLY_REFUND → REFUNDING）。
     * 格口释放待柜机模块开工后补（下单时也没预占，见 WashOrderCreateService 的 TODO）。
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

        releaseCapacity(order);

        // 未支付（WAIT_PAY）取消无需退款；已支付的一律退全款（已确认决策）
        if (from != OrderStatus.WAIT_PAY) {
            OrderStateMachine.TransitionContext refundCtx = OrderStateMachine.TransitionContext.builder()
                    .orderNo(order.getOrderNo())
                    .event(OrderEvent.APPLY_REFUND)
                    .operatorType(OrderOperatorType.JOB)
                    .reason("取消订单自动退款（全额）")
                    .build();
            stateMachine.fire(refundCtx);
            return OrderStatus.REFUNDING;
        }
        return OrderStatus.CANCELED;
    }

    /**
     * 后台人工推进：客服/站长在异常场景下（如柜机故障、司机忘打卡）手动触发事件。
     *
     * <p>能否流转仍由状态机规则表决定，后台只是"换了个触发方"，
     * 不能绕过规则（例如不能在 WAIT_PAY 上触发 QC_PASS）。
     * 每次推进都会写流转日志，操作人留痕，后台可追溯。
     */
    public OrderStatus adminFire(String orderNo, OrderEvent event, Long operatorId, String reason) {
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
