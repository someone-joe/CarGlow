package com.ruoyi.wash.order.domain;

import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 订单主表（最小列集，后续按 PRD 数据模型扩展列，扩展只加不改）。
 * 表 wash_order。status 取值必须是 OrderStatus 枚举名，禁止其他字符串。
 */
public class WashOrder extends BaseEntity {

    private Long orderId;
    private String orderNo;
    private Long serviceId;
    private Long vehicleId;
    private Long cabinetId;
    private Long memberId;
    private Long siteId;
    private Long communityId;
    private String status;
    private String serviceName;
    /** 预约时间，毫秒时间戳 */
    private Long appointTime;
    /** 支付金额，单位：分 */
    private Long payAmount;
    /** 支付截止时间，毫秒时间戳 */
    private Long payExpireAt;
    private String plateNo;
    private String cancelReason;
    private String delFlag;

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

    public Long getCommunityId() {
        return communityId;
    }

    public void setCommunityId(Long communityId) {
        this.communityId = communityId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public Long getAppointTime() {
        return appointTime;
    }

    public void setAppointTime(Long appointTime) {
        this.appointTime = appointTime;
    }

    public Long getServiceId() {
        return serviceId;
    }

    public void setServiceId(Long serviceId) {
        this.serviceId = serviceId;
    }

    public Long getVehicleId() {
        return vehicleId;
    }

    public void setVehicleId(Long vehicleId) {
        this.vehicleId = vehicleId;
    }

    public Long getCabinetId() {
        return cabinetId;
    }

    public void setCabinetId(Long cabinetId) {
        this.cabinetId = cabinetId;
    }

    public Long getPayAmount() {
        return payAmount;
    }

    public void setPayAmount(Long payAmount) {
        this.payAmount = payAmount;
    }

    public Long getPayExpireAt() {
        return payExpireAt;
    }

    public void setPayExpireAt(Long payExpireAt) {
        this.payExpireAt = payExpireAt;
    }

    public String getPlateNo() {
        return plateNo;
    }

    public void setPlateNo(String plateNo) {
        this.plateNo = plateNo;
    }

    public String getCancelReason() {
        return cancelReason;
    }

    public void setCancelReason(String cancelReason) {
        this.cancelReason = cancelReason;
    }

    public String getDelFlag() {
        return delFlag;
    }

    public void setDelFlag(String delFlag) {
        this.delFlag = delFlag;
    }
}
