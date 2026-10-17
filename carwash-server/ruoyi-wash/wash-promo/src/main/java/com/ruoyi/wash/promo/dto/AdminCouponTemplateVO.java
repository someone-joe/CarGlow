package com.ruoyi.wash.promo.dto;

/**
 * 后台券模板行。契约：openapi.yaml AdminCouponTemplateVO。
 *
 * <p>remain（剩余可领）由后端算好下发——前端自己减会出现两套口径，
 * 尤其是"运营刚改完 total，前端还没刷新"的时候。金额单位分，时间毫秒时间戳。
 */
public class AdminCouponTemplateVO {

    private Long couponTemplateId;
    private String name;
    /** NORMAL 普通券 / NEWBIE 新客券 / INVITE 邀请奖励券 */
    private String type;
    private Long thresholdAmount;
    private Long discountAmount;
    private Integer total;
    private Integer issued;
    private Integer remain;
    private Integer perLimit;
    private Long startTime;
    private Long endTime;
    /** Y 上架 / N 下架 */
    private String status;
    private Long createTime;

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

    public Integer getRemain() {
        return remain;
    }

    public void setRemain(Integer remain) {
        this.remain = remain;
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

    public Long getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Long createTime) {
        this.createTime = createTime;
    }
}
