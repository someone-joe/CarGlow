package com.ruoyi.wash.worker.domain;

import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 师傅资质。表 wash_worker_qualification，与师傅一对一（worker_id 唯一）。
 *
 * <p>两道关卡分开记，互不替代：
 * <ul>
 *   <li>faceStatus 人脸核身结果（PENDING/PASSED/FAILED）——由核身渠道回填；</li>
 *   <li>reviewStatus 后台人工审核（PENDING/PASSED/REJECTED）——运营看证件照后拍板。</li>
 * </ul>
 * 派单要求两者都 PASSED。
 */
public class WashWorkerQualification extends BaseEntity {

    private Long id;
    private Long workerId;
    private String idName;
    private String idNo;
    private String idFrontFile;
    private String idBackFile;
    private String licenseFile;
    /** 人脸核身：PENDING / PASSED / FAILED */
    private String faceStatus;
    /** 核身渠道：WECHAT / MANUAL */
    private String faceProvider;
    /** 核身凭证号：微信返回的 verifyId 或人工登记单号 */
    private String faceRef;
    private Long faceTime;
    /** 审核：PENDING / PASSED / REJECTED */
    private String reviewStatus;
    private String reviewRemark;
    private String reviewer;
    private Long submitTime;
    private Long reviewTime;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getWorkerId() {
        return workerId;
    }

    public void setWorkerId(Long workerId) {
        this.workerId = workerId;
    }

    public String getIdName() {
        return idName;
    }

    public void setIdName(String idName) {
        this.idName = idName;
    }

    public String getIdNo() {
        return idNo;
    }

    public void setIdNo(String idNo) {
        this.idNo = idNo;
    }

    public String getIdFrontFile() {
        return idFrontFile;
    }

    public void setIdFrontFile(String idFrontFile) {
        this.idFrontFile = idFrontFile;
    }

    public String getIdBackFile() {
        return idBackFile;
    }

    public void setIdBackFile(String idBackFile) {
        this.idBackFile = idBackFile;
    }

    public String getLicenseFile() {
        return licenseFile;
    }

    public void setLicenseFile(String licenseFile) {
        this.licenseFile = licenseFile;
    }

    public String getFaceStatus() {
        return faceStatus;
    }

    public void setFaceStatus(String faceStatus) {
        this.faceStatus = faceStatus;
    }

    public String getFaceProvider() {
        return faceProvider;
    }

    public void setFaceProvider(String faceProvider) {
        this.faceProvider = faceProvider;
    }

    public String getFaceRef() {
        return faceRef;
    }

    public void setFaceRef(String faceRef) {
        this.faceRef = faceRef;
    }

    public Long getFaceTime() {
        return faceTime;
    }

    public void setFaceTime(Long faceTime) {
        this.faceTime = faceTime;
    }

    public String getReviewStatus() {
        return reviewStatus;
    }

    public void setReviewStatus(String reviewStatus) {
        this.reviewStatus = reviewStatus;
    }

    public String getReviewRemark() {
        return reviewRemark;
    }

    public void setReviewRemark(String reviewRemark) {
        this.reviewRemark = reviewRemark;
    }

    public String getReviewer() {
        return reviewer;
    }

    public void setReviewer(String reviewer) {
        this.reviewer = reviewer;
    }

    public Long getSubmitTime() {
        return submitTime;
    }

    public void setSubmitTime(Long submitTime) {
        this.submitTime = submitTime;
    }

    public Long getReviewTime() {
        return reviewTime;
    }

    public void setReviewTime(Long reviewTime) {
        this.reviewTime = reviewTime;
    }
}
