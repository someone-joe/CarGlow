package com.ruoyi.wash.pay.domain;

/** 充值赠送规则（充 X 送 Y）。后台「钱包配置」整体覆盖维护，充值单存金额快照。 */
public class WashWalletGiftRule {

    private Long ruleId;
    /** 充值门槛（分），如 20000 = 200 元 */
    private Long rechargeFen;
    /** 赠送（分），如 2000 = 20 元 */
    private Long giftFen;
    /** 是否启用 Y/N */
    private String enabled;
    private Integer sortOrder;
    private java.util.Date createTime;
    private java.util.Date updateTime;

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

    public String getEnabled() {
        return enabled;
    }

    public void setEnabled(String enabled) {
        this.enabled = enabled;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
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
