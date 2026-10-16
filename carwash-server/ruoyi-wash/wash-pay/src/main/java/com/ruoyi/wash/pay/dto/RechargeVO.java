package com.ruoyi.wash.pay.dto;

/** 发起充值结果。单位：分。expireAt 为支付截止（毫秒）。 */
public class RechargeVO {
    private String rechargeNo;
    private Long payAmount;
    private Long giftAmount;
    private Long expireAt;

    public RechargeVO() {
    }

    public RechargeVO(String rechargeNo, Long payAmount, Long giftAmount, Long expireAt) {
        this.rechargeNo = rechargeNo;
        this.payAmount = payAmount;
        this.giftAmount = giftAmount;
        this.expireAt = expireAt;
    }

    public String getRechargeNo() {
        return rechargeNo;
    }

    public void setRechargeNo(String rechargeNo) {
        this.rechargeNo = rechargeNo;
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

    public Long getExpireAt() {
        return expireAt;
    }

    public void setExpireAt(Long expireAt) {
        this.expireAt = expireAt;
    }
}
