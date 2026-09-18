package com.ruoyi.wash.member.dto;

/** 后台车辆列表行。车牌属于个人信息，后台按会员维度展示，不做额外脱敏（客服需要报车牌找车）。 */
public class AdminVehicleVO {

    private Long vehicleId;
    private Long memberId;
    private String plateNo;
    private String brand;
    private String color;
    private String isNewEnergy;
    private Long communityId;
    private String parkingNo;
    private String createTime;

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

    public String getCreateTime() {
        return createTime;
    }

    public void setCreateTime(String createTime) {
        this.createTime = createTime;
    }
}
