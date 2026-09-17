package com.ruoyi.wash.order.dto;

/** 影像证据 —— 与 openapi.yaml 的 MediaVO 逐字段一致。影像模块未开工，当前只用于占位空数组。 */
public class MediaVO {

    private String fileId;
    private String bizType;
    private String url;
    private Long uploadTime;

    public String getFileId() {
        return fileId;
    }

    public void setFileId(String fileId) {
        this.fileId = fileId;
    }

    public String getBizType() {
        return bizType;
    }

    public void setBizType(String bizType) {
        this.bizType = bizType;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public Long getUploadTime() {
        return uploadTime;
    }

    public void setUploadTime(Long uploadTime) {
        this.uploadTime = uploadTime;
    }
}
