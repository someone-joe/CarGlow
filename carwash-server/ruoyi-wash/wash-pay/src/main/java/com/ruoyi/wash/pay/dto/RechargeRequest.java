package com.ruoyi.wash.pay.dto;

/** 发起充值。ruleId（选赠送档）与 amountFen（自定义金额）二选一，ruleId 优先。 */
public class RechargeRequest {
    private Long ruleId;
    /** 自定义实付金额（分），未选赠送档时必填 */
    private Long amountFen;

    public Long getRuleId() {
        return ruleId;
    }

    public void setRuleId(Long ruleId) {
        this.ruleId = ruleId;
    }

    public Long getAmountFen() {
        return amountFen;
    }

    public void setAmountFen(Long amountFen) {
        this.amountFen = amountFen;
    }
}
