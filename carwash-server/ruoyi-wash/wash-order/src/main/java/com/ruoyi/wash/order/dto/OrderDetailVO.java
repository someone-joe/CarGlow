package com.ruoyi.wash.order.dto;

import java.util.List;

/** 订单详情 —— 与 openapi.yaml 的 OrderDetailVO 逐字段一致。 */
public class OrderDetailVO {

    private String orderNo;
    private String status;
    private String statusLabel;
    private String statusDesc;
    private Long promiseReturnTime;
    private Boolean overdue;
    private String siteName;
    private String cabinetName;
    private String slotNo;
    private String plateNo;
    private String serviceName;
    private String remark;
    private Long originAmount;
    private Long discountAmount;
    private Long payAmount;
    private String payType;
    private List<TimelineNodeVO> timeline;
    private List<MediaVO> medias;
    private OrderActionVO mainAction;
    private List<OrderActionVO> subActions;

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

    public String getStatusDesc() {
        return statusDesc;
    }

    public void setStatusDesc(String statusDesc) {
        this.statusDesc = statusDesc;
    }

    public Long getPromiseReturnTime() {
        return promiseReturnTime;
    }

    public void setPromiseReturnTime(Long promiseReturnTime) {
        this.promiseReturnTime = promiseReturnTime;
    }

    public Boolean getOverdue() {
        return overdue;
    }

    public void setOverdue(Boolean overdue) {
        this.overdue = overdue;
    }

    public String getSiteName() {
        return siteName;
    }

    public void setSiteName(String siteName) {
        this.siteName = siteName;
    }

    public String getCabinetName() {
        return cabinetName;
    }

    public void setCabinetName(String cabinetName) {
        this.cabinetName = cabinetName;
    }

    public String getSlotNo() {
        return slotNo;
    }

    public void setSlotNo(String slotNo) {
        this.slotNo = slotNo;
    }

    public String getPlateNo() {
        return plateNo;
    }

    public void setPlateNo(String plateNo) {
        this.plateNo = plateNo;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public Long getOriginAmount() {
        return originAmount;
    }

    public void setOriginAmount(Long originAmount) {
        this.originAmount = originAmount;
    }

    public Long getDiscountAmount() {
        return discountAmount;
    }

    public void setDiscountAmount(Long discountAmount) {
        this.discountAmount = discountAmount;
    }

    public Long getPayAmount() {
        return payAmount;
    }

    public void setPayAmount(Long payAmount) {
        this.payAmount = payAmount;
    }

    public String getPayType() {
        return payType;
    }

    public void setPayType(String payType) {
        this.payType = payType;
    }

    public List<TimelineNodeVO> getTimeline() {
        return timeline;
    }

    public void setTimeline(List<TimelineNodeVO> timeline) {
        this.timeline = timeline;
    }

    public List<MediaVO> getMedias() {
        return medias;
    }

    public void setMedias(List<MediaVO> medias) {
        this.medias = medias;
    }

    public OrderActionVO getMainAction() {
        return mainAction;
    }

    public void setMainAction(OrderActionVO mainAction) {
        this.mainAction = mainAction;
    }

    public List<OrderActionVO> getSubActions() {
        return subActions;
    }

    public void setSubActions(List<OrderActionVO> subActions) {
        this.subActions = subActions;
    }
}
