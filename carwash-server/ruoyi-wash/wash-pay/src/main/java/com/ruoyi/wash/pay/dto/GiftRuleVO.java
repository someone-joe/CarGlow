package com.ruoyi.wash.pay.dto;

/** 充值赠送规则（充 X 送 Y）。单位：分。 */
public class GiftRuleVO {
    private Long ruleId;
    private Long rechargeFen;
    private Long giftFen;

    public GiftRuleVO() {
    }

    public GiftRuleVO(Long ruleId, Long rechargeFen, Long giftFen) {
        this.ruleId = ruleId;
        this.rechargeFen = rechargeFen;
        this.giftFen = giftFen;
    }

    public Long getRuleId() {
        return ruleId;
    }

    public void setRuleId(Long ruleId) {
        this.ruleId = ruleId;
    }

    public Long getRechargeFen() {
        return rechargeFen;
    }

    public void setRechargeFen(Long rechargeFen) {
        this.rechargeFen = rechargeFen;
    }

    public Long getGiftFen() {
        return giftFen;
    }

    public void setGiftFen(Long giftFen) {
        this.giftFen = giftFen;
    }
}
