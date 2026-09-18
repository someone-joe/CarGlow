package com.ruoyi.wash.member.dto;

/**
 * 车辆常停位置。契约：openapi.yaml AddressVO。
 *
 * <p>building / unit / floor 目前为 null：wash_vehicle 只有 community_id 与 parking_no，
 * 楼栋单元楼层要等小区地址模块开工再补，不臆造。
 */
public class AddressVO {

    private Long communityId;
    private String building;
    private String unit;
    private String floor;
    private String parkingNo;

    public Long getCommunityId() {
        return communityId;
    }

    public void setCommunityId(Long communityId) {
        this.communityId = communityId;
    }

    public String getBuilding() {
        return building;
    }

    public void setBuilding(String building) {
        this.building = building;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public String getFloor() {
        return floor;
    }

    public void setFloor(String floor) {
        this.floor = floor;
    }

    public String getParkingNo() {
        return parkingNo;
    }

    public void setParkingNo(String parkingNo) {
        this.parkingNo = parkingNo;
    }
}
