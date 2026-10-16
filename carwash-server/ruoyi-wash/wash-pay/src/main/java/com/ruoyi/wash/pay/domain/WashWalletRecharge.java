package com.ruoyi.wash.pay.domain;

/** 储值充值单。金额为快照（实付 + 赠送），赠送规则后续调整不影响历史单。 */
public class WashWalletRecharge {

    private Long rechargeId;
    private String rechargeNo;
    private Long memberId;
    /** 使用的赠送规则 ID，自定义金额为空 */
    private Long ruleId;
    /** 实付金额（分） */
    private Long payAmount;
    /** 赠送金额（分） */
    private Long giftAmount;
    /** 支付渠道：MOCK（微信后补） */
    private String channel;
    /** INIT / SUCCESS / FAILED */
    private String status;
    private String providerTradeNo;
    /** 支付截止（毫秒） */
    private Long payExpireAt;
    private java.util.Date createTime;
    private java.util.Date updateTime;

    public Long getRechargeId() {
        return rechargeId;
    }

    public void setRechargeId(Long rechargeId) {
        this.rechargeId = rechargeId;
    }

    public String getRechargeNo() {
        return rechargeNo;
    }

    public void setRechargeNo(String rechargeNo) {
        this.rechargeNo = rechargeNo;
    }

    public Long getMemberId() {
        return memberId;
    }

    public void setMemberId(Long memberId) {
        this.memberId = memberId;
    }

    public Long getRuleId() {
        return ruleId;
    }

    public void setRuleId(Long ruleId) {
        this.ruleId = ruleId;
    }

    public Long getPayAmount() {
        return payAmount;
    }

    public void setPayAmount(Long payAmount) {
        this.payAmount = payAmount;
    }

    public Long getGiftAmount() {
        return giftAmount;
    }

    public void setGiftAmount(Long giftAmount) {
        this.giftAmount = giftAmount;
    }

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getProviderTradeNo() {
        return providerTradeNo;
    }

    public void setProviderTradeNo(String providerTradeNo) {
        this.providerTradeNo = providerTradeNo;
    }

    public Long getPayExpireAt() {
        return payExpireAt;
    }

    public void setPayExpireAt(Long payExpireAt) {
        this.payExpireAt = payExpireAt;
    }

    public java.util.Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(java.util.Date createTime) {
        this.createTime = createTime;
    }

    public java.util.Date getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(java.util.Date updateTime) {
        this.updateTime = updateTime;
    }
}
