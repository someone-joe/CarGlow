package com.ruoyi.wash.goods.domain;

import com.ruoyi.common.core.domain.BaseEntity;

import java.util.List;

/** 服务项。表 wash_service。金额一律分（priceAmount）。 */
public class WashService extends BaseEntity {

    private Long serviceId;
    private Long categoryId;
    private String serviceName;
    private Long priceAmount;
    private Integer workMinutes;
    private String enabled;

    /** 服务内容清单（JSON 数组，对应 detail_items） */
    private List<String> items;
    /** 不包含项（JSON 数组，对应 detail_not_included） */
    private List<String> notIncluded;
    /** 注意事项（JSON 数组，对应 detail_notices） */
    private List<String> notices;
    /** 样图（JSON 数组，图片 URL，对应 detail_sample_images） */
    private List<String> sampleImages;
    /** 适用车型（对应 applicable_models） */
    private String applicableModels;
    /** 封面图 URL（对应 cover_url） */
    private String cover;

    public Long getServiceId() {
        return serviceId;
    }

    public void setServiceId(Long serviceId) {
        this.serviceId = serviceId;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public Long getPriceAmount() {
        return priceAmount;
    }

    public void setPriceAmount(Long priceAmount) {
        this.priceAmount = priceAmount;
    }

    public Integer getWorkMinutes() {
        return workMinutes;
    }

    public void setWorkMinutes(Integer workMinutes) {
        this.workMinutes = workMinutes;
    }

    public String getEnabled() {
        return enabled;
    }

    public void setEnabled(String enabled) {
        this.enabled = enabled;
    }

    public List<String> getItems() {
        return items;
    }

    public void setItems(List<String> items) {
        this.items = items;
    }

    public List<String> getNotIncluded() {
        return notIncluded;
    }

    public void setNotIncluded(List<String> notIncluded) {
        this.notIncluded = notIncluded;
    }

    public List<String> getNotices() {
        return notices;
    }

    public void setNotices(List<String> notices) {
        this.notices = notices;
    }

    public List<String> getSampleImages() {
        return sampleImages;
    }

    public void setSampleImages(List<String> sampleImages) {
        this.sampleImages = sampleImages;
    }

    public String getApplicableModels() {
        return applicableModels;
    }

    public void setApplicableModels(String applicableModels) {
        this.applicableModels = applicableModels;
    }

    public String getCover() {
        return cover;
    }

    public void setCover(String cover) {
        this.cover = cover;
    }
}
