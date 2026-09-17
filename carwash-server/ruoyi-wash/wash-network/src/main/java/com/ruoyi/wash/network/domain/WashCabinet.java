package com.ruoyi.wash.network.domain;

import com.ruoyi.common.core.domain.BaseEntity;

/** 智能钥匙柜。表 wash_cabinet。格口（slot）相关能力待柜机模块开工后补齐。 */
public class WashCabinet extends BaseEntity {

    private Long cabinetId;
    private Long siteId;
    private Long communityId;
    private String cabinetName;
    private String enabled;

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
}
