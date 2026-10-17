package com.ruoyi.wash.order.dto;

/**
 * 后台售后列表行 = {@link AfterSaleVO} + 订单上下文。
 *
 * <p>为什么要多带订单上下文：运营处置时不能只有一句"客户想退款"，
 * 得同时看到这单现在什么状态、买的什么服务，否则容易误批（比如还没洗的单纯改约被当成全额退款）。
 * 手机号等敏感字段一律不带（个保法最小化，与后台订单列表同一口径）。
 */
public class AdminAfterSaleVO extends AfterSaleVO {

    private Long memberId;
    private String orderStatus;
    private String serviceName;

    public Long getMemberId() {
        return memberId;
    }

    public void setMemberId(Long memberId) {
        this.memberId = memberId;
    }

    public String getOrderStatus() {
        return orderStatus;
    }

    public void setOrderStatus(String orderStatus) {
        this.orderStatus = orderStatus;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }
}
