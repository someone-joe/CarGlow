package com.ruoyi.wash.common.statemachine;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 订单状态机 —— 全项目唯一允许修改订单状态的入口（闸机）。
 *
 * <p>流程：加锁 → 读当前状态 → 校验合法性 → CAS 更新 → 通知副作用 → 解锁。
 * 任何业务代码都不得绕过本类直接 update 订单状态。
 *
 * <p>本类不依赖 Spring，可脱离框架单测；
 * 加锁、持久化、副作用三个能力通过构造注入，接入时由 RuoYi 提供实现。
 */
public class OrderStateMachine {

    private final Lock lock;
    private final Accessor accessor;
    private final List<Listener> listeners = new CopyOnWriteArrayList<>();

    public OrderStateMachine(Lock lock, Accessor accessor) {
        this.lock = lock;
        this.accessor = accessor;
    }

    /** 注册副作用监听器（写流转日志、发消息、释放格口、回补产能等），按注册顺序执行。 */
    public void addListener(Listener listener) {
        listeners.add(listener);
    }

    /**
     * 发起一次状态流转。
     *
     * @param ctx 流转上下文，见 {@link TransitionContext}
     * @return 流转后的新状态
     * @throws OrderStatusException 状态非法、已被并发修改、或权限不足
     */
    public OrderStatus fire(TransitionContext ctx) {
        String orderNo = ctx.getOrderNo();
        lock.lock(orderNo);
        try {
            OrderStatus current = accessor.loadStatus(orderNo);
            if (current == null) {
                throw new OrderStatusException("订单不存在：" + orderNo);
            }
            if (current.isTerminal()) {
                throw new OrderStatusException("订单已处于终态，不可再流转：" + current);
            }
            // 调用方若声明了期望的起始状态，不一致说明订单已被别处改过，直接失败而不是覆盖
            if (ctx.getExpectFrom() != null && ctx.getExpectFrom() != current) {
                throw new OrderStatusException("订单状态已变更，期望 " + ctx.getExpectFrom() + "，实际 " + current);
            }
            if (!OrderStatusTransitions.canFire(current, ctx.getEvent(), ctx.getOperatorType())) {
                throw new OrderStatusException("不允许的流转：" + current + " + " + ctx.getEvent()
                        + "（触发方 " + ctx.getOperatorType() + "）");
            }

            OrderStatus next = OrderStatusTransitions.next(current, ctx.getEvent())
                    .orElseThrow(() -> new OrderStatusException("不允许的流转：" + current + " + " + ctx.getEvent()));

            // CAS 更新：状态被并发改过时 affected 为 0，直接报错由上层重试
            if (!accessor.updateStatus(orderNo, current, next)) {
                throw new OrderStatusException("订单状态已被并发修改，请重试：" + orderNo);
            }

            for (Listener listener : listeners) {
                listener.onTransition(ctx, current, next);
            }
            return next;
        } finally {
            lock.unlock(orderNo);
        }
    }

    /** 判断能否流转，供按钮渲染与前置校验使用，不产生副作用。 */
    public boolean canFire(OrderStatus from, OrderEvent event, OrderOperatorType operatorType) {
        return OrderStatusTransitions.canFire(from, event, operatorType);
    }

    /** 分布式锁：接入时用 Redis 实现，粒度为订单号。 */
    public interface Lock {
        void lock(String orderNo);

        void unlock(String orderNo);
    }

    /** 订单状态读写：接入时用 MyBatis + 事务实现。 */
    public interface Accessor {
        OrderStatus loadStatus(String orderNo);

        /**
         * 乐观更新，仅当库中状态仍为 from 时才更新为 to。
         *
         * @return 是否更新成功
         */
        boolean updateStatus(String orderNo, OrderStatus from, OrderStatus to);
    }

    /** 流转成功后的副作用。实现内部异常不得吞掉，应记录并告警。 */
    public interface Listener {
        void onTransition(TransitionContext ctx, OrderStatus from, OrderStatus to);
    }

    /** 流转上下文。 */
    public static class TransitionContext {
        private final String orderNo;
        private final OrderEvent event;
        private final OrderOperatorType operatorType;
        private final Long operatorId;
        private final String reason;
        /** 期望的起始状态，可为空表示不校验。 */
        private final OrderStatus expectFrom;

        private TransitionContext(Builder builder) {
            this.orderNo = builder.orderNo;
            this.event = builder.event;
            this.operatorType = builder.operatorType;
            this.operatorId = builder.operatorId;
            this.reason = builder.reason;
            this.expectFrom = builder.expectFrom;
        }

        public static Builder builder() {
            return new Builder();
        }

        public String getOrderNo() {
            return orderNo;
        }

        public OrderEvent getEvent() {
            return event;
        }

        public OrderOperatorType getOperatorType() {
            return operatorType;
        }

        public Long getOperatorId() {
            return operatorId;
        }

        public String getReason() {
            return reason;
        }

        public OrderStatus getExpectFrom() {
            return expectFrom;
        }

        public static class Builder {
            private String orderNo;
            private OrderEvent event;
            private OrderOperatorType operatorType;
            private Long operatorId;
            private String reason;
            private OrderStatus expectFrom;

            public Builder orderNo(String orderNo) {
                this.orderNo = orderNo;
                return this;
            }

            public Builder event(OrderEvent event) {
                this.event = event;
                return this;
            }

            public Builder operatorType(OrderOperatorType operatorType) {
                this.operatorType = operatorType;
                return this;
            }

            public Builder operatorId(Long operatorId) {
                this.operatorId = operatorId;
                return this;
            }

            public Builder reason(String reason) {
                this.reason = reason;
                return this;
            }

            public Builder expectFrom(OrderStatus expectFrom) {
                this.expectFrom = expectFrom;
                return this;
            }

            public TransitionContext build() {
                if (orderNo == null || event == null || operatorType == null) {
                    throw new OrderStatusException("流转上下文缺少必填项：orderNo / event / operatorType");
                }
                return new TransitionContext(this);
            }
        }
    }
}
