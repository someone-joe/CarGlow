package com.ruoyi.wash.worker.dto;

/**
 * 师傅任务池单项 —— 与 openapi.yaml 的 PickTaskVO 逐字段一致，禁止另立字段名。
 *
 * <p>车辆品牌/颜色、客户称呼与电话、取送地址、承诺还车时间等字段依赖其他域的数据接入，
 * 当前统一返回 null，前端不展示空值即可。
 */
public class PickTaskVO {

    private String orderNo;
    /** PICKUP 待取车 / RETURN 待送回 */
    private String bizType;
    private String status;
    private String serviceName;
    private String vehiclePlate;
    private String vehicleBrand;
    private String vehicleColor;
    private String customerName;
    private String customerPhone;
    private String pickupAddr;
    private String returnAddr;
    private String appointmentDate;
    private String promiseReturnTime;
    private String pickupDeadline;
    private Integer requiredPhotoCount;
    private Integer distanceMeters;

    public String getOrderNo() {
        return orderNo;
    }

    public void setOrderNo(String orderNo) {
        this.orderNo = orderNo;
    }

    public String getBizType() {
        return bizType;
    }

    public void setBizType(String bizType) {
        this.bizType = bizType;
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

    public String getVehiclePlate() {
        return vehiclePlate;
    }

    public void setVehiclePlate(String vehiclePlate) {
        this.vehiclePlate = vehiclePlate;
    }

    public String getVehicleBrand() {
        return vehicleBrand;
    }

    public void setVehicleBrand(String vehicleBrand) {
        this.vehicleBrand = vehicleBrand;
    }

    public String getVehicleColor() {
        return vehicleColor;
    }

    public void setVehicleColor(String vehicleColor) {
        this.vehicleColor = vehicleColor;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getCustomerPhone() {
        return customerPhone;
    }

    public void setCustomerPhone(String customerPhone) {
        this.customerPhone = customerPhone;
    }

    public String getPickupAddr() {
        return pickupAddr;
    }

    public void setPickupAddr(String pickupAddr) {
        this.pickupAddr = pickupAddr;
    }

    public String getReturnAddr() {
        return returnAddr;
    }

    public void setReturnAddr(String returnAddr) {
        this.returnAddr = returnAddr;
    }

    public String getAppointmentDate() {
        return appointmentDate;
    }

    public void setAppointmentDate(String appointmentDate) {
        this.appointmentDate = appointmentDate;
    }

    public String getPromiseReturnTime() {
        return promiseReturnTime;
    }

    public void setPromiseReturnTime(String promiseReturnTime) {
        this.promiseReturnTime = promiseReturnTime;
    }

    public String getPickupDeadline() {
        return pickupDeadline;
    }

    public void setPickupDeadline(String pickupDeadline) {
        this.pickupDeadline = pickupDeadline;
    }

    public Integer getRequiredPhotoCount() {
        return requiredPhotoCount;
    }

    public void setRequiredPhotoCount(Integer requiredPhotoCount) {
        this.requiredPhotoCount = requiredPhotoCount;
    }

    public Integer getDistanceMeters() {
        return distanceMeters;
    }

    public void setDistanceMeters(Integer distanceMeters) {
        this.distanceMeters = distanceMeters;
    }
}
