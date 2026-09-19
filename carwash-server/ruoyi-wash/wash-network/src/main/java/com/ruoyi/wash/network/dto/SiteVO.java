package com.ruoyi.wash.network.dto;

/** 站点信息（C 端顶部站点栏）。契约：openapi.yaml /api/v1/site/current 的 SiteVO。 */
public class SiteVO {

    private Long siteId;
    private String siteName;
    private Long communityId;
    private String communityName;
    /** 展示文案：小区名 · 站点名，如「泷景花园 · 泷景中央站」 */
    private String displayName;
    private String serviceStatus;
    private String closedNotice;

    public Long getSiteId() {
        return siteId;
    }

    public void setSiteId(Long siteId) {
        this.siteId = siteId;
    }

    public String getSiteName() {
        return siteName;
    }

    public void setSiteName(String siteName) {
        this.siteName = siteName;
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

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getServiceStatus() {
        return serviceStatus;
    }

    public void setServiceStatus(String serviceStatus) {
        this.serviceStatus = serviceStatus;
    }

    public String getClosedNotice() {
        return closedNotice;
    }

    public void setClosedNotice(String closedNotice) {
        this.closedNotice = closedNotice;
    }
}
