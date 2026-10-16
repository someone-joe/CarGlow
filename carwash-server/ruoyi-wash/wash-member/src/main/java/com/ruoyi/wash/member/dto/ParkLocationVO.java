package com.ruoyi.wash.member.dto;

/** 常用停车位置。 */
public class ParkLocationVO {
    private Long locationId;
    private Long communityId;
    private String communityName;
    private String building;
    private String parkingNo;
    private String detail;
    private String isDefault;

    public ParkLocationVO() {
    }

    public ParkLocationVO(Long locationId, Long communityId, String communityName, String building,
                          String parkingNo, String detail, String isDefault) {
        this.locationId = locationId;
        this.communityId = communityId;
        this.communityName = communityName;
        this.building = building;
        this.parkingNo = parkingNo;
        this.detail = detail;
        this.isDefault = isDefault;
    }

    public Long getLocationId() {
        return locationId;
    }

    public void setLocationId(Long locationId) {
        this.locationId = locationId;
    }

    public Long getCommunityId() {
        return communityId;
    }

    public void setCommunityId(Long communityId) {
        this.communityId = communityId;
    }

    public String getCommunityName() {
        return communityName;
    }

    public void setCommunityName(String communityName) {
        this.communityName = communityName;
    }

    public String getBuilding() {
        return building;
    }

    public void setBuilding(String building) {
        this.building = building;
    }

    public String getParkingNo() {
        return parkingNo;
    }

    public void setParkingNo(String parkingNo) {
        this.parkingNo = parkingNo;
    }

    public String getDetail() {
        return detail;
    }

    public void setDetail(String detail) {
        this.detail = detail;
    }

    public String getIsDefault() {
        return isDefault;
    }

    public void setIsDefault(String isDefault) {
        this.isDefault = isDefault;
    }
}
