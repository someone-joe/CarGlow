package com.ruoyi.wash.pay.dto;

/**
 * 退款单出参。契约：openapi.yaml RefundVO。
 *
 * <p>这是"钱到底退没退"的唯一口径：订单状态 REFUNDING 只表示在处理中，
 * 真正的结果看本 VO 的 status（SUCCESS 才是钱已回到客户账上）。
 *
 * <p>为什么不直接返回 WashRefundOrder 实体：它继承了若依 BaseEntity，
 * createTime 会被若依的 Jackson 配置序列化成 yyyy-MM-dd HH:mm:ss 字符串，
 * 与本项目"时间一律毫秒时间戳"的约定不一致。用 VO 把转换收在一处。
 */
public class RefundVO {

    private String refundNo;
    private String orderNo;
    /** 退款金额（分） */
    private Long amount;
    private String reason;
    /** INIT 待退款 / SUCCESS 已退 / FAILED 失败待重试 */
    private String status;
    /** 状态中文名，由后端枚举给出（前端只展示不翻译） */
    private String statusLabel;
    /** 渠道：mock / wechat */
    private String provider;
    /** 渠道退款流水号，对账用 */
    private String providerTradeNo;
    private String failReason;
    private Integer retryCount;
    private Long createTime;
    private Long updateTime;

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

    public String getStatusLabel() {
        return statusLabel;
    }

    public void setStatusLabel(String statusLabel) {
        this.statusLabel = statusLabel;
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

    public Long getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Long createTime) {
        this.createTime = createTime;
    }

    public Long getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Long updateTime) {
        this.updateTime = updateTime;
    }
}
