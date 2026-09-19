package com.ruoyi.wash.order.domain;

/** 影像证据。表 wash_media。fileId 对外，store_path 只在服务端使用，不暴露给前端。 */
public class WashMedia {

    private Long mediaId;
    private String fileId;
    /** PARK / PICK / WASHED / RETURN / COMPARE / VIDEO */
    private String bizType;
    private String orderNo;
    private Long memberId;
    private String fileName;
    private String storePath;
    private Long fileSize;
    /** CUSTOMER / WORKER / ADMIN / DEVICE */
    private String uploadBy;

    public Long getMediaId() {
        return mediaId;
    }

    public void setMediaId(Long mediaId) {
        this.mediaId = mediaId;
    }

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

    public String getOrderNo() {
        return orderNo;
    }

    public void setOrderNo(String orderNo) {
        this.orderNo = orderNo;
    }

    public Long getMemberId() {
        return memberId;
    }

    public void setMemberId(Long memberId) {
        this.memberId = memberId;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getStorePath() {
        return storePath;
    }

    public void setStorePath(String storePath) {
        this.storePath = storePath;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }

    public String getUploadBy() {
        return uploadBy;
    }

    public void setUploadBy(String uploadBy) {
        this.uploadBy = uploadBy;
    }
}
