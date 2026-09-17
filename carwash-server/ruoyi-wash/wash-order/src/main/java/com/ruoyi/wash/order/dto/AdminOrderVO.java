package com.ruoyi.wash.order.dto;

/**
 * 后台订单列表行。注意：后台看得到全量订单，手机号等敏感字段在此**不返回**，
 * 需要时单独走脱敏接口（个保法最小化原则，技术方案 P8）。
 */
public class AdminOrderVO {

    private Long orderId;
    private String orderNo;
    private String status;
    private String statusLabel;
    private String serviceName;
    private String plateNo;
    /** 金额，单位：分 */
    private Long payAmount;
    private Long memberId;
    private Long siteId;
    private Long communityId;
    private Long appointTime;
    private String createTime;

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

    public Long getPayAmount() {
        return payAmount;
    }

    public void setPayAmount(Long payAmount) {
        this.payAmount = payAmount;
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

    public Long getAppointTime() {
        return appointTime;
    }

    public void setAppointTime(Long appointTime) {
        this.appointTime = appointTime;
    }

    public String getCreateTime() {
        return createTime;
    }

    public void setCreateTime(String createTime) {
        this.createTime = createTime;
    }
}
