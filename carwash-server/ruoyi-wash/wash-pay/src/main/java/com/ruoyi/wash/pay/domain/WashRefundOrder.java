package com.ruoyi.wash.pay.domain;

import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 退款单。表 wash_order_refund。
 *
 * <p>是否真的把钱退回去了，以本表 status 为准；订单状态 REFUNDING 只表示"处理中"。
 * 渠道调用失败会留在 INIT/FAILED，由定时任务重试，不丢单。
 */
public class WashRefundOrder extends BaseEntity {

    private Long refundId;
    private String refundNo;
    private String orderNo;
    private Long memberId;
    /** 退款金额，单位：分 */
    private Long amount;
    private String reason;
    /** INIT 待退款 / SUCCESS 已退 / FAILED 失败待重试 */
    private String status;
    private String provider;
    private String providerTradeNo;
    private String failReason;
    private Integer retryCount;

    public Long getRefundId() {
        return refundId;
    }

    public void setRefundId(Long refundId) {
        this.refundId = refundId;
    }

    public String getRefundNo() {
        return refundNo;
    }

    public void setRefundNo(String refundNo) {
        this.refundNo = refundNo;
    }

    public String getOrderNo() {
        return orderNo;
    }

    public void setOrderNo(String orderNo) {
        this.orderNo = orderNo;
    }

    public Long getMemberId() {
        return memberId;
    }

    public void setMemberId(Long memberId) {
        this.memberId = memberId;
    }

    public Long getAmount() {
        return amount;
    }

    public void setAmount(Long amount) {
        this.amount = amount;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getProviderTradeNo() {
        return providerTradeNo;
    }

    public void setProviderTradeNo(String providerTradeNo) {
        this.providerTradeNo = providerTradeNo;
    }

    public String getFailReason() {
        return failReason;
    }

    public void setFailReason(String failReason) {
        this.failReason = failReason;
    }

    public Integer getRetryCount() {
        return retryCount;
    }

    public void setRetryCount(Integer retryCount) {
        this.retryCount = retryCount;
    }
}
