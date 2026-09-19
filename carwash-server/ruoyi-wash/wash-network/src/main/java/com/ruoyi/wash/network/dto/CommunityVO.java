package com.ruoyi.wash.network.dto;

/** 已开通小区。契约：openapi.yaml /api/v1/communities 的 CommunityVO。 */
public class CommunityVO {

    private Long communityId;
    private String name;
    /** 地库通行授权，false 时禁止该小区下单 */
    private Boolean accessAuth;
    private Boolean enabled;

    public Long getCommunityId() {
        return communityId;
    }

    public void setCommunityId(Long communityId) {
        this.communityId = communityId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Boolean getAccessAuth() {
        return accessAuth;
    }

    public void setAccessAuth(Boolean accessAuth) {
        this.accessAuth = accessAuth;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }
}
