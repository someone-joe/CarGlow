package com.ruoyi.wash.promo.dto;

/** 后台批量发放明细行。手机号后端脱敏。 */
public class AdminCouponBatchItemVO {

    private Long itemId;
    private Long memberId;
    private String memberPhone;
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

    public Long getMemberId() {
        return memberId;
    }

    public void setMemberId(Long memberId) {
        this.memberId = memberId;
    }

    public String getMemberPhone() {
        return memberPhone;
    }

    public void setMemberPhone(String memberPhone) {
        this.memberPhone = memberPhone;
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
