package com.ruoyi.wash.member.domain;

import com.ruoyi.common.core.domain.BaseEntity;

/** 会员车辆。表 wash_vehicle。 */
public class WashVehicle extends BaseEntity {

    private Long vehicleId;
    private Long memberId;
    private String plateNo;
    private String brand;
    private String color;
    private String isNewEnergy;
    private Long communityId;
    private String parkingNo;

    public Long getVehicleId() {
        return vehicleId;
    }

    public void setVehicleId(Long vehicleId) {
        this.vehicleId = vehicleId;
    }

    public Long getMemberId() {
        return memberId;
    }

    public void setMemberId(Long memberId) {
        this.memberId = memberId;
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

    public String getIsNewEnergy() {
        return isNewEnergy;
    }

    public void setIsNewEnergy(String isNewEnergy) {
        this.isNewEnergy = isNewEnergy;
    }

    public Long getCommunityId() {
        return communityId;
    }

    public void setCommunityId(Long communityId) {
        this.communityId = communityId;
    }

    public String getParkingNo() {
        return parkingNo;
    }

    public void setParkingNo(String parkingNo) {
        this.parkingNo = parkingNo;
    }
}
