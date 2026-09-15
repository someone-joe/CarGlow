package com.ruoyi.wash.common.statemachine;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

/**
 * 订单状态流转规则表 —— 「起始状态 + 事件 → 目标状态」的唯一定义处。
 *
 * <p>本表必须与《订单状态机规则表.md》第二节逐条一致；不一致即为代码漂移。
 * 类加载时做自校验：重复规则、引用不存在的状态都会在启动阶段直接失败，
 * 避免带着错误规则上线。
 */
public final class OrderStatusTransitions {

    private static final Map<OrderStatus, Map<OrderEvent, OrderStatus>> TABLE = build();

    private OrderStatusTransitions() {
    }

    private static Map<OrderStatus, Map<OrderEvent, OrderStatus>> build() {
        Map<OrderStatus, Map<OrderEvent, OrderStatus>> table = new EnumMap<>(OrderStatus.class);

        // ---------- 正常主链路 ----------
        rule(table, OrderStatus.WAIT_PAY, OrderEvent.PAY_SUCCESS, OrderStatus.WAIT_KEY);
        rule(table, OrderStatus.WAIT_KEY, OrderEvent.DEPOSIT_KEY, OrderStatus.KEY_IN);
        rule(table, OrderStatus.KEY_IN, OrderEvent.TAKE_KEY, OrderStatus.PICKING);
        rule(table, OrderStatus.PICKING, OrderEvent.PICK_CAR_DONE, OrderStatus.TO_STATION);
        rule(table, OrderStatus.TO_STATION, OrderEvent.ARRIVE_STATION, OrderStatus.WASHING);
        rule(table, OrderStatus.WASHING, OrderEvent.SOP_DONE, OrderStatus.QC);
        rule(table, OrderStatus.QC, OrderEvent.QC_FAIL, OrderStatus.WASHING);
        rule(table, OrderStatus.QC, OrderEvent.QC_PASS, OrderStatus.WAIT_RETURN);
        rule(table, OrderStatus.WAIT_RETURN, OrderEvent.LEAVE_STATION, OrderStatus.RETURNING);
        rule(table, OrderStatus.RETURNING, OrderEvent.RETURN_DONE, OrderStatus.RETURNED);
        rule(table, OrderStatus.RETURNED, OrderEvent.TAKE_KEY_BACK, OrderStatus.WAIT_REVIEW);
        rule(table, OrderStatus.WAIT_REVIEW, OrderEvent.REVIEW_SUBMIT, OrderStatus.FINISHED);
        rule(table, OrderStatus.WAIT_REVIEW, OrderEvent.AUTO_FINISH, OrderStatus.FINISHED);

        // ---------- 顺延（自环：状态不变，仅改预约日期） ----------
        rule(table, OrderStatus.WAIT_KEY, OrderEvent.POSTPONE, OrderStatus.WAIT_KEY);

        // ---------- 取消：除终态与已取消/退款中/已退款外的 11 个状态均可 ----------
        for (OrderStatus from : OrderStatus.values()) {
            if (from == OrderStatus.FINISHED || from == OrderStatus.CANCELED
                    || from == OrderStatus.REFUNDING || from == OrderStatus.REFUNDED) {
                continue;
            }
            rule(table, from, OrderEvent.CANCEL, OrderStatus.CANCELED);
        }

        // ---------- 退款 ----------
        rule(table, OrderStatus.CANCELED, OrderEvent.APPLY_REFUND, OrderStatus.REFUNDING);
        rule(table, OrderStatus.WAIT_REVIEW, OrderEvent.APPLY_REFUND, OrderStatus.REFUNDING);
        rule(table, OrderStatus.FINISHED, OrderEvent.APPLY_REFUND, OrderStatus.REFUNDING);
        rule(table, OrderStatus.REFUNDING, OrderEvent.REFUND_SUCCESS, OrderStatus.REFUNDED);

        return Collections.unmodifiableMap(table);
    }

    private static void rule(Map<OrderStatus, Map<OrderEvent, OrderStatus>> table,
                             OrderStatus from, OrderEvent event, OrderStatus to) {
        Map<OrderEvent, OrderStatus> events = table.computeIfAbsent(from, k -> new EnumMap<>(OrderEvent.class));
        if (events.put(event, to) != null) {
            throw new IllegalStateException("重复的流转规则：" + from + " + " + event);
        }
    }

    /**
     * 查询目标状态；规则不存在时返回 empty，由状态机抛异常。
     */
    public static Optional<OrderStatus> next(OrderStatus from, OrderEvent event) {
        Map<OrderEvent, OrderStatus> events = TABLE.get(from);
        if (events == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(events.get(event));
    }

    /**
     * 判断某触发方能否在指定状态下发起某事件。
     * 用于 C 端按钮渲染与后端二次校验共用同一套判断，避免前后端判断不一致。
     */
    public static boolean canFire(OrderStatus from, OrderEvent event, OrderOperatorType operatorType) {
        if (from == null || event == null || operatorType == null) {
            return false;
        }
        if (from.isTerminal()) {
            return false;
        }
        if (!next(from, event).isPresent()) {
            return false;
        }
        // 客户自助取消仅限车未移动的 4 个状态
        if (event == OrderEvent.CANCEL && operatorType == OrderOperatorType.CUSTOMER) {
            return from.isCustomerCancelable();
        }
        return true;
    }
}
