package com.ruoyi.wash.member.dto;

/**
 * 会员车辆（C 端）。契约：openapi.yaml /api/v1/vehicles 的 VehicleVO。
 *
 * <p>photoUrl 为 null（影像模块未开工）；isDefault 为 null（wash_vehicle 无 is_default 列），
 * 前端按列表首项预选即可，不要自己发明"默认车"规则。
 */
public class VehicleVO {

    private Long vehicleId;
    private String plateNo;
    private String brand;
    private String color;
    private String photoUrl;
    private Boolean isNewEnergy;
    private Boolean isDefault;
    private AddressVO address;

    public Long getVehicleId() {
        return vehicleId;
    }

    public void setVehicleId(Long vehicleId) {
        this.vehicleId = vehicleId;
    }

    public String getPlateNo() {
        return plateNo;
    }

    public void setPlateNo(String plateNo) {
        this.plateNo = plateNo;
    }

    public String getBrand() {
        return brand;
    }

    public void setBrand(String brand) {
        this.brand = brand;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public String getPhotoUrl() {
        return photoUrl;
    }

    public void setPhotoUrl(String photoUrl) {
        this.photoUrl = photoUrl;
    }

    public Boolean getIsNewEnergy() {
        return isNewEnergy;
    }

    public void setIsNewEnergy(Boolean isNewEnergy) {
        this.isNewEnergy = isNewEnergy;
    }

    public Boolean getIsDefault() {
        return isDefault;
    }

    public void setIsDefault(Boolean isDefault) {
        this.isDefault = isDefault;
    }

    public AddressVO getAddress() {
        return address;
    }

    public void setAddress(AddressVO address) {
        this.address = address;
    }
}
