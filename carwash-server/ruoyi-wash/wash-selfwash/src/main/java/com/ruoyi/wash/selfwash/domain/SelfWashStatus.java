package com.ruoyi.wash.selfwash.domain;

/**
 * 自助洗车订单状态 —— 本域唯一定义处（与无感 wash_order 的 OrderStatus 完全隔离）。
 *
 * <p>主链路：WAIT_PAY(待支付) → WAIT_USE(待使用) → [到店激活] QUEUING(排队中)/SERVING(服务中) → DONE(已完成)。
 * 异常：CANCELED(已取消) / PAUSED(设备故障暂停)。
 */
public enum SelfWashStatus {

    WAIT_PAY("待支付"),
    WAIT_USE("待使用"),
    QUEUING("排队中"),
    SERVING("服务中"),
    PAUSED("设备故障暂停"),
    DONE("已完成"),
    CANCELED("已取消");

    private final String label;

    SelfWashStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public static SelfWashStatus of(String value) {
        for (SelfWashStatus s : values()) {
            if (s.name().equals(value)) {
                return s;
            }
        }
        throw new IllegalArgumentException("未知的自助洗车订单状态：" + value);
    }
}
