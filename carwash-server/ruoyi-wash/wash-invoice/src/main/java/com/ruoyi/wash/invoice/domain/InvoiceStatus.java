package com.ruoyi.wash.invoice.domain;

/** 发票申请状态 —— 本域唯一定义处。 */
public enum InvoiceStatus {

    APPLYING("申请中"),
    ISSUED("已开票"),
    REJECTED("已驳回");

    private final String label;

    InvoiceStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public static InvoiceStatus of(String value) {
        for (InvoiceStatus s : values()) {
            if (s.name().equals(value)) {
                return s;
            }
        }
        throw new IllegalArgumentException("未知的发票状态：" + value);
    }
}
