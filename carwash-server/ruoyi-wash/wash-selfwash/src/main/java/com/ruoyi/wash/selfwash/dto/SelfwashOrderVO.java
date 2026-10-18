package com.ruoyi.wash.selfwash.dto;

import java.util.Date;

/** 自助洗车订单（C 端）。金额单位：分；时间为 ms 或 Date。 */
public class SelfwashOrderVO {
    private String orderNo;
    private Long siteId;
    private Long workstationId;
    private String serviceName;
    private String plateNo;
    /** 车型快照：SEDAN/SUV/MPV/OTHER */
    private String vehicleType;
    /** 车型中文名，后端枚举给出，前端只展示不翻译 */
    private String vehicleTypeLabel;
    private Integer serviceMinutes;
    private Long payAmount;
    private String payChannel;
    private String status;
    private String statusLabel;
    private Integer queueNo;
    private Long startedAt;
    private Long expireAt;
    private Integer addedSeconds;
    private Long payExpireAt;
    private String cancelReason;
    private Long couponUserId;
    private Long couponDiscount;
    private Date createTime;
    /** 服务剩余秒（SERVING 时 >0，其余 0） */
    private Long remainingSeconds;

    public String getOrderNo() {
        return orderNo;
    }

    public void setOrderNo(String orderNo) {
        this.orderNo = orderNo;
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

    public String getVehicleTypeLabel() {
        return vehicleTypeLabel;
    }

    public void setVehicleTypeLabel(String vehicleTypeLabel) {
        this.vehicleTypeLabel = vehicleTypeLabel;
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

    public String getStatusLabel() {
        return statusLabel;
    }

    public void setStatusLabel(String statusLabel) {
        this.statusLabel = statusLabel;
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

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public Long getRemainingSeconds() {
        return remainingSeconds;
    }

    public void setRemainingSeconds(Long remainingSeconds) {
        this.remainingSeconds = remainingSeconds;
    }
}
