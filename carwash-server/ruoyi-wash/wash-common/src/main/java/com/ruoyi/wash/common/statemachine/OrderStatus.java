package com.ruoyi.wash.common.statemachine;

/**
 * 订单状态枚举 —— 全项目唯一定义处（SSOT）。
 *
 * <p>定义来源：《订单状态机规则表.md》第一节，上游为 PRD V2.0 表 4-1。
 * 共 15 个状态：主链路 12 个 + 取消/退款 3 个。
 *
 * <p>禁止在业务代码里写状态字符串字面量，一律使用本枚举。
 * 修改本枚举前，必须先修改《订单状态机规则表.md》。
 */
public enum OrderStatus {

    WAIT_PAY("待支付", false, true),
    WAIT_KEY("待存钥匙", false, true),
    KEY_IN("钥匙已入柜", false, true),
    PICKING("取车中", false, true),
    TO_STATION("运输中（去程）", false, false),
    WASHING("清洗中", false, false),
    QC("待质检", false, false),
    WAIT_RETURN("待还车", false, false),
    RETURNING("运输中（回程）", false, false),
    RETURNED("已还车（待取钥匙）", false, false),
    WAIT_REVIEW("待评价", false, false),
    FINISHED("已完成", true, false),
    CANCELED("已取消", false, false),
    REFUNDING("退款中", false, false),
    REFUNDED("已退款", true, false);

    /** C 端展示名 */
    private final String label;

    /**
     * 是否终态 —— 仅用于 C 端展示（订单是否已结束、是否显示"再来一单"）。
     *
     * <p>流转能否进行一律以《订单状态机规则表》为准，不以本字段一刀切拦截：
     * 「已完成」是展示上的终态，但业务上仍需支持退款（已确认决策），
     * 真正没有任何出边规则的状态是「已退款」。
     */
    private final boolean terminal;

    /**
     * 客户是否可自助取消。
     * 仅车尚未移动的 4 个状态为 true；之后只能由客服/站长在后台取消（PRD 9：已在清洗禁止客户取回）。
     */
    private final boolean customerCancelable;

    OrderStatus(String label, boolean terminal, boolean customerCancelable) {
        this.label = label;
        this.terminal = terminal;
        this.customerCancelable = customerCancelable;
    }

    public String getLabel() {
        return label;
    }

    public boolean isTerminal() {
        return terminal;
    }

    public boolean isCustomerCancelable() {
        return customerCancelable;
    }

    /**
     * 按状态值解析枚举，解析失败直接抛异常而不是返回 null，避免空指针漂移。
     */
    public static OrderStatus of(String value) {
        for (OrderStatus status : values()) {
            if (status.name().equals(value)) {
                return status;
            }
        }
        throw new OrderStatusException("未知的订单状态：" + value);
    }
}
