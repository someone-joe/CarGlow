package com.ruoyi.wash.order.domain;

/**
 * 售后工单状态枚举 —— 唯一定义处（SSOT），与 openapi.yaml 的 AfterSaleVO.status 逐条一致。
 *
 * <p>为什么不把中文名散落在各 Service / 前端：C 端「售后进度」页与后台处置页都要展示，
 * 各写一份必然漂移。后端统一给 label，前端只展示不翻译（与 OrderStatus 同一口径）。
 *
 * <p>EXPIRED 这类「计算出来的状态」不要放进来——那是查询时按时间算的，不是工单的真实状态。
 */
public enum AfterSaleStatus {

    /** 待处理：客户已提交，运营还没看 */
    INIT("待处理"),
    /** 处理中：已同意（REFUND 类已发起退款，或 REWASH/CLAIM 转人工） */
    PROCESSING("处理中"),
    /** 已完成：退款已到账或人工已处理完 */
    DONE("已完成"),
    /** 已驳回：运营不同意，客户看得到原因 */
    REJECTED("已驳回");

    private final String label;

    AfterSaleStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    /** 按枚举名解析；未知值返回 null，由调用方决定回落成原值还是忽略 */
    public static AfterSaleStatus of(String name) {
        if (name == null) {
            return null;
        }
        for (AfterSaleStatus s : values()) {
            if (s.name().equals(name)) {
                return s;
            }
        }
        return null;
    }

    /** 取展示名：未知状态原样返回，别把脏数据显示成空白 */
    public static String labelOf(String name) {
        AfterSaleStatus s = of(name);
        return s == null ? name : s.getLabel();
    }
}
