package com.ruoyi.wash.selfwash.domain;

import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 自助洗车订单。表 wash_selfwash_order。与无感 wash_order 完全独立。
 *
 * <p>计时计费：SERVING 时 started_at + (serviceMinutes + addedSeconds) * 60000 = expire_at；
 * 到点由定时任务置 DONE（软件闭环），并触发设备「断电」mock。
 */
public class WashSelfwashOrder extends BaseEntity {

    private Long orderId;
    private String orderNo;
    private Long memberId;
    private Long siteId;
    private Long workstationId;
    private Long serviceId;
    /** 服务名称快照 */
    private String serviceName;
    /** 车牌快照（排队展示/监控用，必填） */
    private String plateNo;
    /** 车型快照：SEDAN/SUV/MPV/OTHER，运营调度工位与水量用 */
    private String vehicleType;
    /** 付费时长（分钟）快照 */
    private Integer serviceMinutes;
    /** 实付金额（分）快照 */
    private Long payAmount;
    /** 支付渠道：BALANCE 储值 / WECHAT_MOCK 微信(本地mock) */
    private String payChannel;
    /** 状态：SelfWashStatus */
    private String status;
    /** 排队序号（QUEUING 时） */
    private Integer queueNo;
    /** 服务开始时间（ms） */
    private Long startedAt;
    /** 服务截止时间（ms） */
    private Long expireAt;
    /** 累计加时（秒） */
    private Integer addedSeconds;
    /** 支付截止（ms） */
    private Long payExpireAt;
    private String cancelReason;
    /** 使用的优惠券ID（核销快照） */
    private Long couponUserId;
    /** 优惠券抵扣金额（分） */
    private Long couponDiscount;

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public String getOrderNo() {
        return orderNo;
    }

    public void setOrderNo(String orderNo) {
        this.orderNo = orderNo;
    }

    public Long getMemberId() {
        return memberId;
    }

    public void setMemberId(Long memberId) {
        this.memberId = memberId;
    }

    public Long getSiteId() {
        return siteId;
    }

    public void setSiteId(Long siteId) {
        this.siteId = siteId;
    }

    public Long getWorkstationId() {
        return workstationId;
    }

    public void setWorkstationId(Long workstationId) {
        this.workstationId = workstationId;
    }

    public Long getServiceId() {
        return serviceId;
    }

    public void setServiceId(Long serviceId) {
        this.serviceId = serviceId;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public String getPlateNo() {
        return plateNo;
    }

    public void setPlateNo(String plateNo) {
        this.plateNo = plateNo;
    }

    public String getVehicleType() {
        return vehicleType;
    }

    public void setVehicleType(String vehicleType) {
        this.vehicleType = vehicleType;
    }

    public Integer getServiceMinutes() {
        return serviceMinutes;
    }

    public void setServiceMinutes(Integer serviceMinutes) {
        this.serviceMinutes = serviceMinutes;
    }

    public Long getPayAmount() {
        return payAmount;
    }

    public void setPayAmount(Long payAmount) {
        this.payAmount = payAmount;
    }

    public String getPayChannel() {
        return payChannel;
    }

    public void setPayChannel(String payChannel) {
        this.payChannel = payChannel;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getQueueNo() {
        return queueNo;
    }

    public void setQueueNo(Integer queueNo) {
        this.queueNo = queueNo;
    }

    public Long getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(Long startedAt) {
        this.startedAt = startedAt;
    }

    public Long getExpireAt() {
        return expireAt;
    }

    public void setExpireAt(Long expireAt) {
        this.expireAt = expireAt;
    }

    public Integer getAddedSeconds() {
        return addedSeconds;
    }

    public void setAddedSeconds(Integer addedSeconds) {
        this.addedSeconds = addedSeconds;
    }

    public Long getPayExpireAt() {
        return payExpireAt;
    }

    public void setPayExpireAt(Long payExpireAt) {
        this.payExpireAt = payExpireAt;
    }

    public String getCancelReason() {
        return cancelReason;
    }

    public void setCancelReason(String cancelReason) {
        this.cancelReason = cancelReason;
    }

    public Long getCouponUserId() {
        return couponUserId;
    }

    public void setCouponUserId(Long couponUserId) {
        this.couponUserId = couponUserId;
    }

    public Long getCouponDiscount() {
        return couponDiscount;
    }

    public void setCouponDiscount(Long couponDiscount) {
        this.couponDiscount = couponDiscount;
    }
}
