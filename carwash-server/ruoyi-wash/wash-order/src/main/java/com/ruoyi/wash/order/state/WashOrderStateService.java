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
import org.springframework.stereotype.Service;

/**
 * 订单状态流转的唯一入口 —— 业务代码只允许调用本服务，禁止直接 update 状态。
 *
 * <p>把 wash-common 里的状态机闸机接入真实环境：
 * 锁 = Redis、状态读写 = MyBatis、副作用 = 写流转日志（只增不删）。
 */
@Service
public class WashOrderStateService {

    private final OrderRedisLock lock;
    private final WashOrderStatusAccessor accessor;
    private final WashOrderMapper orderMapper;
    private OrderStateMachine stateMachine;

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
