package com.ruoyi.wash.promo.domain;

/** 优惠券批量发放明细：每个会员一条当前结果。表 wash_coupon_batch_item。 */
public class WashCouponBatchItem {

    private Long itemId;
    private Long batchId;
    private Long templateId;
    private Long memberId;
    private String result;
    private String reason;
    private Integer grantedCount;
    private Long createTime;

    public Long getItemId() {
        return itemId;
    }

    public void setItemId(Long itemId) {
        this.itemId = itemId;
    }

    public Long getBatchId() {
        return batchId;
    }

    public void setBatchId(Long batchId) {
        this.batchId = batchId;
    }

    public Long getTemplateId() {
        return templateId;
    }

    public void setTemplateId(Long templateId) {
        this.templateId = templateId;
    }

    public Long getMemberId() {
        return memberId;
    }

    public void setMemberId(Long memberId) {
        this.memberId = memberId;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public Integer getGrantedCount() {
        return grantedCount;
    }

    public void setGrantedCount(Integer grantedCount) {
        this.grantedCount = grantedCount;
    }

    public Long getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Long createTime) {
        this.createTime = createTime;
    }
}
