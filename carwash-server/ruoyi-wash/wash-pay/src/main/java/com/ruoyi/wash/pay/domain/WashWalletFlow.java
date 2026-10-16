package com.ruoyi.wash.pay.domain;

/** 钱包流水（充值/赠送/支付/退款）。只增不删，变动后余额快照便于对账。 */
public class WashWalletFlow {

    private Long flowId;
    private String flowNo;
    private Long memberId;
    /** RECHARGE / GIFT / PAY / REFUND */
    private String bizType;
    /** 本金变动（分，正入负出） */
    private Long changeAmount;
    /** 赠送变动（分，正入负出） */
    private Long giftChange;
    /** 变动后本金（分） */
    private Long balanceAfter;
    /** 变动后赠送（分） */
    private Long giftAfter;
    /** 关联单号（recharge_no / order_no） */
    private String bizNo;
    private String remark;
    private java.util.Date createTime;

    public Long getFlowId() {
        return flowId;
    }

    public void setFlowId(Long flowId) {
        this.flowId = flowId;
    }

    public String getFlowNo() {
        return flowNo;
    }

    public void setFlowNo(String flowNo) {
        this.flowNo = flowNo;
    }

    public Long getMemberId() {
        return memberId;
    }

    public void setMemberId(Long memberId) {
        this.memberId = memberId;
    }

    public String getBizType() {
        return bizType;
    }

    public void setBizType(String bizType) {
        this.bizType = bizType;
    }

    public Long getChangeAmount() {
        return changeAmount;
    }

    public void setChangeAmount(Long changeAmount) {
        this.changeAmount = changeAmount;
    }

    public Long getGiftChange() {
        return giftChange;
    }

    public void setGiftChange(Long giftChange) {
        this.giftChange = giftChange;
    }

    public Long getBalanceAfter() {
        return balanceAfter;
    }

    public void setBalanceAfter(Long balanceAfter) {
        this.balanceAfter = balanceAfter;
    }

    public Long getGiftAfter() {
        return giftAfter;
    }

    public void setGiftAfter(Long giftAfter) {
        this.giftAfter = giftAfter;
    }

    public String getBizNo() {
        return bizNo;
    }

    public void setBizNo(String bizNo) {
        this.bizNo = bizNo;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public java.util.Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(java.util.Date createTime) {
        this.createTime = createTime;
    }
}
