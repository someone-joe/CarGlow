package com.ruoyi.wash.network.domain;

import com.ruoyi.common.core.domain.BaseEntity;

/** 智能钥匙柜。表 wash_cabinet。 */
public class WashCabinet extends BaseEntity {

    private Long cabinetId;
    private Long siteId;
    private Long communityId;
    private String cabinetName;
    private String enabled;

    /** 列表展示用：关联站点名 / 小区名（非表字段） */
    private String siteName;
    private String communityName;

    public Long getCabinetId() {
        return cabinetId;
    }

    public void setCabinetId(Long cabinetId) {
        this.cabinetId = cabinetId;
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

    public String getCabinetName() {
        return cabinetName;
    }

    public void setCabinetName(String cabinetName) {
        this.cabinetName = cabinetName;
    }

    public String getEnabled() {
        return enabled;
    }

    public void setEnabled(String enabled) {
        this.enabled = enabled;
    }

    public String getSiteName() {
        return siteName;
    }

    public void setSiteName(String siteName) {
        this.siteName = siteName;
    }

    public String getCommunityName() {
        return communityName;
    }

    public void setCommunityName(String communityName) {
        this.communityName = communityName;
    }
}
