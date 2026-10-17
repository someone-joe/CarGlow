package com.ruoyi.wash.promo.dto;

/**
 * 首页 Banner（C 端）。契约：openapi.yaml /api/v1/banners 的 BannerVO。
 *
 * <p>imageUrl 可为空：无图时 C 端渲染「标题 + 副标题」文字图。
 */
public class BannerVO {

    private Long bannerId;
    private String imageUrl;
    private String title;
    private String subtitle;
    /** NONE / ACTIVITY / RECHARGE / SERVICE */
    private String linkType;
    private String linkValue;
    private Integer sort;

    public Long getBannerId() {
        return bannerId;
    }

    public void setBannerId(Long bannerId) {
        this.bannerId = bannerId;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSubtitle() {
        return subtitle;
    }

    public void setSubtitle(String subtitle) {
        this.subtitle = subtitle;
    }

    public String getLinkType() {
        return linkType;
    }

    public void setLinkType(String linkType) {
        this.linkType = linkType;
    }

    public String getLinkValue() {
        return linkValue;
    }

    public void setLinkValue(String linkValue) {
        this.linkValue = linkValue;
    }

    public Integer getSort() {
        return sort;
    }

    public void setSort(Integer sort) {
        this.sort = sort;
    }
}
