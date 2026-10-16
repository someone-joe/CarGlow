package com.ruoyi.wash.notify.domain;

/**
 * 订阅消息模板（需求 11）—— 本域唯一定义处。
 *
 * <p>configKey 对应配置项 wash.notify.templates.&lt;configKey&gt;（模板ID，留空表示未配置）。
 * monthlyCap：每月最多推送条数，0 表示不限（需求 11.5：活动推送每月不超过 4 条）。
 */
public enum NotifyTemplate {

    ORDER_STATUS("orderStatus", "订单状态变更", 0),
    KEY_READY("keyReady", "车辆已洗好待取钥匙", 0),
    COUPON_EXPIRE("couponExpire", "优惠券到期提醒", 0),
    BALANCE_LOW("balanceLow", "储值余额不足提醒", 0),
    ACTIVITY("activity", "活动推送", 4);

    private final String configKey;
    private final String label;
    private final int monthlyCap;

    NotifyTemplate(String configKey, String label, int monthlyCap) {
        this.configKey = configKey;
        this.label = label;
        this.monthlyCap = monthlyCap;
    }

    public String configKey() {
        return configKey;
    }

    public String getLabel() {
        return label;
    }

    public int monthlyCap() {
        return monthlyCap;
    }

    public static NotifyTemplate of(String name) {
        for (NotifyTemplate t : values()) {
            if (t.name().equals(name)) {
                return t;
            }
        }
        throw new IllegalArgumentException("未知的通知模板：" + name);
    }
}
