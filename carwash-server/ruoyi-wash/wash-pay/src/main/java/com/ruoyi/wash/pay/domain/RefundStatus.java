package com.ruoyi.wash.pay.domain;

/**
 * 退款单状态枚举 —— 唯一定义处（SSOT），与 openapi.yaml 的 RefundVO.status 逐条一致。
 *
 * <p>这是"钱到底退没退"的唯一口径：订单状态 REFUNDING 只表示处理中，
 * 只有本枚举的 SUCCESS 才代表钱已经回到客户账上。C 端「退款进度」与后台退款单页共用，
 * 后端给 label，前端只展示不翻译。
 */
public enum RefundStatus {

    /** 待退款：退款单已建，尚未调渠道或还在等结果 */
    INIT("待退款"),
    /** 已退款：渠道确认成功 */
    SUCCESS("已退款"),
    /** 失败待重试：渠道调用失败，系统每 5 分钟自动重试，超限需人工 */
    FAILED("失败待重试");

    private final String label;

    RefundStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public static RefundStatus of(String name) {
        if (name == null) {
            return null;
        }
        for (RefundStatus s : values()) {
            if (s.name().equals(name)) {
                return s;
            }
        }
        return null;
    }

    /** 取展示名：未知状态原样返回，别把脏数据显示成空白 */
    public static String labelOf(String name) {
        RefundStatus s = of(name);
        return s == null ? name : s.getLabel();
    }
}
