package com.ruoyi.wash.promo.domain;

import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 首页 Banner/轮播。表 wash_banner。
 *
 * <p>scene 预留（当前只有 HOME）；imageUrl 可为空 —— 空图时 C 端渲染
 * 「标题 + 副标题」文字图，避免没有素材时首页轮播空档。
 */
public class WashBanner extends BaseEntity {

    private Long bannerId;
    /** 展示场景：HOME 首页 */
    private String scene;
    private String title;
    private String subtitle;
    private String imageUrl;
    /** 跳转类型：NONE / ACTIVITY / RECHARGE / SERVICE */
    private String linkType;
    /** 跳转值：SERVICE 时传 serviceId；其余由前端约定 */
    private String linkValue;
    private Integer sortOrder;
    /** 生效时间（毫秒），为空表示长期有效 */
    private Long startTime;
    private Long endTime;
    /** Y 上架 / N 下架 */
    private String enabled;

    public Long getBannerId() {
        return bannerId;
    }

    public void setBannerId(Long bannerId) {
        this.bannerId = bannerId;
    }

    public String getScene() {
        return scene;
    }

    public void setScene(String scene) {
        this.scene = scene;
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

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
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

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }

    public Long getStartTime() {
        return startTime;
    }

    public void setStartTime(Long startTime) {
        this.startTime = startTime;
    }

    public Long getEndTime() {
        return endTime;
    }

    public void setEndTime(Long endTime) {
        this.endTime = endTime;
    }

    public String getEnabled() {
        return enabled;
    }

    public void setEnabled(String enabled) {
        this.enabled = enabled;
    }
}
