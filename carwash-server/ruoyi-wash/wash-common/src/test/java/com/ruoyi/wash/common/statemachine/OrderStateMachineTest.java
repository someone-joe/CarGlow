package com.ruoyi.wash.common.statemachine;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 订单状态机测试 —— 防漂移的第一道回归网。
 *
 * <p>测试目标（《订单状态机规则表.md》是唯一真源）：
 * <ol>
 *   <li>主链路 12 步能一路走通；</li>
 *   <li>质检打回、顺延自环、取消/退款等旁路正确；</li>
 *   <li>非法流转、越权取消、并发覆盖一律被拒；</li>
 *   <li>已确认的业务决策（已完成仍可退款）在代码里确实生效。</li>
 * </ol>
 */
@DisplayName("订单状态机")
class OrderStateMachineTest {

    private static final String ORDER_NO = "T202609170001";

    /** 内存版订单状态读写，模拟数据库，便于脱离 Spring 单测。 */
    private static class MemAccessor implements OrderStateMachine.Accessor {

        private OrderStatus status;
        private boolean forceUpdateFailure = false;

        MemAccessor(OrderStatus status) {
            this.status = status;
        }

        @Override
        public OrderStatus loadStatus(String orderNo) {
            return status;
        }

        @Override
        public boolean updateStatus(String orderNo, OrderStatus from, OrderStatus to) {
            if (forceUpdateFailure || status != from) {
                return false;
            }
            status = to;
            return true;
        }
    }

    /** 单测用空锁，锁的正确性由接入时的 Redis 实现保证。 */
    private static class NoLock implements OrderStateMachine.Lock {
        @Override
        public void lock(String orderNo) {
        }

        @Override
        public void unlock(String orderNo) {
        }
    }

    private MemAccessor accessor;
    private OrderStateMachine machine;

    private OrderStateMachine machineOf(OrderStatus initial) {
        accessor = new MemAccessor(initial);
        machine = new OrderStateMachine(new NoLock(), accessor);
        return machine;
    }

    private OrderStateMachine.TransitionContext ctx(OrderEvent event, OrderOperatorType operatorType) {
        return OrderStateMachine.TransitionContext.builder()
                .orderNo(ORDER_NO)
                .event(event)
                .operatorType(operatorType)
                .operatorId(1L)
                .build();
    }

    @Test
    @DisplayName("主链路：待支付 → 已完成，12 步全通")
    void happyPath() {
        machineOf(OrderStatus.WAIT_PAY);
        assertEquals(OrderStatus.WAIT_KEY, machine.fire(ctx(OrderEvent.PAY_SUCCESS, OrderOperatorType.JOB)));
        assertEquals(OrderStatus.KEY_IN, machine.fire(ctx(OrderEvent.DEPOSIT_KEY, OrderOperatorType.DEVICE)));
        assertEquals(OrderStatus.PICKING, machine.fire(ctx(OrderEvent.TAKE_KEY, OrderOperatorType.PICKER)));
        assertEquals(OrderStatus.TO_STATION, machine.fire(ctx(OrderEvent.PICK_CAR_DONE, OrderOperatorType.PICKER)));
        assertEquals(OrderStatus.WASHING, machine.fire(ctx(OrderEvent.ARRIVE_STATION, OrderOperatorType.STATION)));
        assertEquals(OrderStatus.QC, machine.fire(ctx(OrderEvent.SOP_DONE, OrderOperatorType.STATION)));
        assertEquals(OrderStatus.WAIT_RETURN, machine.fire(ctx(OrderEvent.QC_PASS, OrderOperatorType.STATION)));
        assertEquals(OrderStatus.RETURNING, machine.fire(ctx(OrderEvent.LEAVE_STATION, OrderOperatorType.PICKER)));
        assertEquals(OrderStatus.RETURNED, machine.fire(ctx(OrderEvent.RETURN_DONE, OrderOperatorType.PICKER)));
        assertEquals(OrderStatus.WAIT_REVIEW, machine.fire(ctx(OrderEvent.TAKE_KEY_BACK, OrderOperatorType.CUSTOMER)));
        assertEquals(OrderStatus.FINISHED, machine.fire(ctx(OrderEvent.REVIEW_SUBMIT, OrderOperatorType.CUSTOMER)));
    }

    @Test
    @DisplayName("超时未评价：自动完成走 AUTO_FINISH")
    void autoFinish() {
        machineOf(OrderStatus.WAIT_REVIEW);
        assertEquals(OrderStatus.FINISHED, machine.fire(ctx(OrderEvent.AUTO_FINISH, OrderOperatorType.JOB)));
    }

    @Test
    @DisplayName("质检不合格：打回重洗后可再次提交")
    void qcFailRollback() {
        machineOf(OrderStatus.WASHING);
        assertEquals(OrderStatus.QC, machine.fire(ctx(OrderEvent.SOP_DONE, OrderOperatorType.STATION)));
        assertEquals(OrderStatus.WASHING, machine.fire(ctx(OrderEvent.QC_FAIL, OrderOperatorType.STATION)));
        assertEquals(OrderStatus.QC, machine.fire(ctx(OrderEvent.SOP_DONE, OrderOperatorType.STATION)));
    }

    @Test
    @DisplayName("顺延：待存钥匙自环，状态不变")
    void postponeSelfLoop() {
        machineOf(OrderStatus.WAIT_KEY);
        assertEquals(OrderStatus.WAIT_KEY, machine.fire(ctx(OrderEvent.POSTPONE, OrderOperatorType.JOB)));
    }

    @Test
    @DisplayName("客户自助取消：车未动可取消，已开洗被拒")
    void customerCancelRule() {
        machineOf(OrderStatus.PICKING);
        assertEquals(OrderStatus.CANCELED, machine.fire(ctx(OrderEvent.CANCEL, OrderOperatorType.CUSTOMER)));

        machineOf(OrderStatus.WASHING);
        assertThrows(OrderStatusException.class,
                () -> machine.fire(ctx(OrderEvent.CANCEL, OrderOperatorType.CUSTOMER)),
                "清洗中不允许客户自助取消（PRD 9：已在清洗禁止客户取回）");

        // 客服/站长可以取消
        assertEquals(OrderStatus.CANCELED, machine.fire(ctx(OrderEvent.CANCEL, OrderOperatorType.ADMIN)));
    }

    @Test
    @DisplayName("已完成仍可退款（业务决策：已确认）")
    void finishedCanRefund() {
        machineOf(OrderStatus.FINISHED);
        assertEquals(OrderStatus.REFUNDING, machine.fire(ctx(OrderEvent.APPLY_REFUND, OrderOperatorType.ADMIN)));
        assertEquals(OrderStatus.REFUNDED, machine.fire(ctx(OrderEvent.REFUND_SUCCESS, OrderOperatorType.ADMIN)));
    }

    @Test
    @DisplayName("取消后退款：已取消 → 退款中 → 已退款")
    void cancelThenRefund() {
        machineOf(OrderStatus.WAIT_PAY);
        assertEquals(OrderStatus.CANCELED, machine.fire(ctx(OrderEvent.CANCEL, OrderOperatorType.CUSTOMER)));
        assertEquals(OrderStatus.REFUNDING, machine.fire(ctx(OrderEvent.APPLY_REFUND, OrderOperatorType.JOB)));
        assertEquals(OrderStatus.REFUNDED, machine.fire(ctx(OrderEvent.REFUND_SUCCESS, OrderOperatorType.JOB)));
    }

    @Test
    @DisplayName("已退款是真终态：任何事件都不再放行")
    void refundedIsTerminal() {
        machineOf(OrderStatus.REFUNDED);
        for (OrderEvent event : OrderEvent.values()) {
            assertThrows(OrderStatusException.class,
                    () -> machine.fire(ctx(event, OrderOperatorType.ADMIN)),
                    "已退款后仍放行了事件：" + event);
        }
    }

    @Test
    @DisplayName("规则表之外的流转一律拒绝（待支付不能直接跳到清洗中）")
    void illegalTransitionRejected() {
        machineOf(OrderStatus.WAIT_PAY);
        assertThrows(OrderStatusException.class,
                () -> machine.fire(ctx(OrderEvent.ARRIVE_STATION, OrderOperatorType.STATION)));
        assertFalse(machine.canFire(OrderStatus.WAIT_PAY, OrderEvent.ARRIVE_STATION, OrderOperatorType.STATION));
    }

    @Test
    @DisplayName("期望起始状态与库中不一致时拒绝，绝不覆盖")
    void expectFromMismatchRejected() {
        machineOf(OrderStatus.WAIT_PAY);
        OrderStateMachine.TransitionContext ctx = OrderStateMachine.TransitionContext.builder()
                .orderNo(ORDER_NO)
                .event(OrderEvent.PAY_SUCCESS)
                .operatorType(OrderOperatorType.JOB)
                .expectFrom(OrderStatus.WAIT_KEY)
                .build();
        assertThrows(OrderStatusException.class, () -> machine.fire(ctx));
        assertEquals(OrderStatus.WAIT_PAY, accessor.loadStatus(ORDER_NO), "拒绝后状态必须保持原样");
    }

    @Test
    @DisplayName("并发导致 CAS 更新失败时报错，不静默成功")
    void casFailureRejected() {
        machineOf(OrderStatus.WAIT_PAY);
        accessor.forceUpdateFailure = true;
        assertThrows(OrderStatusException.class,
                () -> machine.fire(ctx(OrderEvent.PAY_SUCCESS, OrderOperatorType.JOB)));
        assertEquals(OrderStatus.WAIT_PAY, accessor.loadStatus(ORDER_NO));
    }

    @Test
    @DisplayName("订单不存在时拒绝")
    void orderNotFoundRejected() {
        machine = new OrderStateMachine(new NoLock(), new OrderStateMachine.Accessor() {
            @Override
            public OrderStatus loadStatus(String orderNo) {
                return null;
            }

            @Override
            public boolean updateStatus(String orderNo, OrderStatus from, OrderStatus to) {
                return false;
            }
        });
        assertThrows(OrderStatusException.class,
                () -> machine.fire(ctx(OrderEvent.PAY_SUCCESS, OrderOperatorType.JOB)));
    }

    @Test
    @DisplayName("流转成功后监听器按注册顺序收到通知，且收到正确的起止状态")
    void listenerNotified() {
        machineOf(OrderStatus.WAIT_PAY);
        List<String> calls = new ArrayList<>();
        machine.addListener((c, from, to) -> calls.add("first:" + from + "->" + to));
        machine.addListener((c, from, to) -> calls.add("second:" + from + "->" + to));

        machine.fire(ctx(OrderEvent.PAY_SUCCESS, OrderOperatorType.JOB));

        assertEquals(2, calls.size());
        assertEquals("first:WAIT_PAY->WAIT_KEY", calls.get(0));
        assertEquals("second:WAIT_PAY->WAIT_KEY", calls.get(1));
    }

    @Test
    @DisplayName("canFire 不产生副作用：只判断不改状态")
    void canFireHasNoSideEffect() {
        machineOf(OrderStatus.WAIT_PAY);
        assertTrue(machine.canFire(OrderStatus.WAIT_PAY, OrderEvent.PAY_SUCCESS, OrderOperatorType.JOB));
        assertEquals(OrderStatus.WAIT_PAY, accessor.loadStatus(ORDER_NO));
    }

    @Test
    @DisplayName("上下文缺必填项时直接报错")
    void contextValidation() {
        assertThrows(OrderStatusException.class, () -> OrderStateMachine.TransitionContext.builder()
                .event(OrderEvent.PAY_SUCCESS)
                .operatorType(OrderOperatorType.JOB)
                .build());
    }
}
