package com.ruoyi.wash.pay.dto;

import java.util.Date;

/** 钱包流水。单位：分。changeAmount/giftChange 正入负出。 */
public class WalletFlowVO {
    private String flowNo;
    private String bizType;
    private Long changeAmount;
    private Long giftChange;
    private Long balanceAfter;
    private Long giftAfter;
    private String bizNo;
    private String remark;
    private Date createTime;

    public String getFlowNo() {
        return flowNo;
    }

    public void setFlowNo(String flowNo) {
        this.flowNo = flowNo;
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

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }
}
