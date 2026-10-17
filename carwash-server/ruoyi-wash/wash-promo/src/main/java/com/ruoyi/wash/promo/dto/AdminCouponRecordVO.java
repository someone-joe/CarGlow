package com.ruoyi.wash.promo.dto;

/**
 * 后台领用 / 核销记录行。契约：openapi.yaml AdminCouponRecordVO。
 *
 * <p>会员手机号在 SQL 层就脱敏（<code>138****1234</code>），不把明文带进 Java 层：
 * 与后台订单 / 会员列表同一口径（个保法最小化）。金额单位分，时间毫秒时间戳。
 */
public class AdminCouponRecordVO {

    private Long couponUserId;
    private Long couponTemplateId;
    private String templateName;
    private Long memberId;
    private String memberPhone;
    /** UNUSED 未使用 / USED 已核销 / EXPIRED 已过期（过期是计算值，不是库里的状态） */
    private String status;
    private String orderNo;
    private Long obtainTime;
    private Long usedTime;
    private Long expireTime;

    public Long getCouponUserId() {
        return couponUserId;
    }

    public void setCouponUserId(Long couponUserId) {
        this.couponUserId = couponUserId;
    }

    public Long getCouponTemplateId() {
        return couponTemplateId;
    }

    public void setCouponTemplateId(Long couponTemplateId) {
        this.couponTemplateId = couponTemplateId;
    }

    public String getTemplateName() {
        return templateName;
    }

    public void setTemplateName(String templateName) {
        this.templateName = templateName;
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getOrderNo() {
        return orderNo;
    }

    public void setOrderNo(String orderNo) {
        this.orderNo = orderNo;
    }

    public Long getObtainTime() {
        return obtainTime;
    }

    public void setObtainTime(Long obtainTime) {
        this.obtainTime = obtainTime;
    }

    public Long getUsedTime() {
        return usedTime;
    }

    public void setUsedTime(Long usedTime) {
        this.usedTime = usedTime;
    }

    public Long getExpireTime() {
        return expireTime;
    }

    public void setExpireTime(Long expireTime) {
        this.expireTime = expireTime;
    }
}
