package com.ruoyi.wash.promo.domain;

import com.ruoyi.common.core.domain.BaseEntity;

/** 优惠券模板。表 wash_coupon_template。金额单位：分；时间：毫秒时间戳。 */
public class WashCouponTemplate extends BaseEntity {

    private Long couponTemplateId;
    private String name;
    /** NORMAL / NEWBIE */
    private String type;
    /** 使用门槛（分），0 = 无门槛 */
    private Long thresholdAmount;
    /** 减免金额（分） */
    private Long discountAmount;
    /** 发行总量 */
    private Integer total;
    /** 已领取 */
    private Integer issued;
    /** 每人限领 */
    private Integer perLimit;
    /** 生效时间（毫秒） */
    private Long startTime;
    /** 失效时间（毫秒） */
    private Long endTime;
    /** Y 上架 / N 下架 */
    private String status;

    public Long getCouponTemplateId() {
        return couponTemplateId;
    }

    public void setCouponTemplateId(Long couponTemplateId) {
        this.couponTemplateId = couponTemplateId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Long getThresholdAmount() {
        return thresholdAmount;
    }

    public void setThresholdAmount(Long thresholdAmount) {
        this.thresholdAmount = thresholdAmount;
    }

    public Long getDiscountAmount() {
        return discountAmount;
    }

    public void setDiscountAmount(Long discountAmount) {
        this.discountAmount = discountAmount;
    }

    public Integer getTotal() {
        return total;
    }

    public void setTotal(Integer total) {
        this.total = total;
    }

    public Integer getIssued() {
        return issued;
    }

    public void setIssued(Integer issued) {
        this.issued = issued;
    }

    public Integer getPerLimit() {
        return perLimit;
    }

    public void setPerLimit(Integer perLimit) {
        this.perLimit = perLimit;
    }

    public Long getStartTime() {
        return startTime;
    }

    public void setStartTime(Long startTime) {
        this.startTime = startTime;
    }

    public Long getEndTime() {
        return endTime;
    }

    public void setEndTime(Long endTime) {
        this.endTime = endTime;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
