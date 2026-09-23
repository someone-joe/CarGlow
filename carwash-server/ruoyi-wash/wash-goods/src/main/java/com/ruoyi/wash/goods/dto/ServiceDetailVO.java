package com.ruoyi.wash.goods.dto;

import java.util.List;

/**
 * C 端服务详情 —— 与契约 ServiceDetailVO 一致（ServiceVO 全部字段 + 明细字段）。
 *
 * <p>items / notices / 样图等明细字段当前返回空：wash_service 表还没有这些列，
 * 待后台「服务详情配置」能力落地（加列 + 后台表单）后填充，前端已有空态兜底。
 */
public class ServiceDetailVO extends ServiceVO {

    /** 服务内容清单 */
    private List<String> items;
    private String applicableModels;
    private List<String> notIncluded;
    private List<String> notices;
    private List<String> sampleImages;

    public List<String> getItems() {
        return items;
    }

    public void setItems(List<String> items) {
        this.items = items;
    }

    public String getApplicableModels() {
        return applicableModels;
    }

    public void setApplicableModels(String applicableModels) {
        this.applicableModels = applicableModels;
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
}
