package com.ruoyi.wash.member.domain;

import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 会员常用停车位置。表 wash_park_location。
 *
 * <p>无感洗车下单时选一个已存位置，避免每次重填楼栋/车位。communityName 为选择时快照，便于展示。
 */
public class WashParkLocation extends BaseEntity {

    private Long locationId;
    private Long memberId;
    /** 所在小区ID */
    private Long communityId;
    /** 所在小区名称（快照） */
    private String communityName;
    /** 楼栋/单元号 */
    private String building;
    /** 车位号，如 B2区A柱旁 / A区23号 */
    private String parkingNo;
    /** 具体位置补充 */
    private String detail;
    /** 默认位置（Y/N） */
    private String isDefault;

    public Long getLocationId() {
        return locationId;
    }

    public void setLocationId(Long locationId) {
        this.locationId = locationId;
    }

    public Long getMemberId() {
        return memberId;
    }

    public void setMemberId(Long memberId) {
        this.memberId = memberId;
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
