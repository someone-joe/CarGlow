package com.ruoyi.wash.promo.domain;

import com.ruoyi.common.core.domain.BaseEntity;

/** 用户优惠券。表 wash_coupon_user。状态：UNUSED / USED / EXPIRED。 */
public class WashCouponUser extends BaseEntity {

    private Long couponUserId;
    private Long templateId;
    private Long memberId;
    /** UNUSED / USED / EXPIRED */
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
