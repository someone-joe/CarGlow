package com.ruoyi.wash.worker.service;

import com.ruoyi.wash.common.api.ApiException;
import com.ruoyi.wash.common.api.ErrorCode;
import com.ruoyi.wash.worker.domain.WashWorkerQualification;
import com.ruoyi.wash.worker.mapper.WashWorkerQualificationMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 师傅资质：师傅提交证件 → 后台审核；人脸核身结果单独回填。
 *
 * <p>派单要求 faceStatus=PASSED 且 reviewStatus=PASSED（见 WashDispatchService）。
 * 真实微信人脸核身需微信侧资质，未接入前由后台按渠道登记结果（faceProvider=MANUAL），
 * 接入微信后同一字段回填 WECHAT 的 verifyId，数据模型不变。
 */
@Service
public class WashWorkerQualificationService {

    @Autowired
    private WashWorkerQualificationMapper qualificationMapper;

    /** 师傅查看自己的资质（不存在时返回一份空态，前端据此引导去提交） */
    public Map<String, Object> my(Long workerId) {
        WashWorkerQualification q = qualificationMapper.selectByWorkerId(workerId);
        Map<String, Object> out = new HashMap<>();
        if (q == null) {
            out.put("submitted", false);
            out.put("faceStatus", "PENDING");
            out.put("reviewStatus", "PENDING");
            return out;
        }
        out.put("submitted", true);
        out.put("idName", q.getIdName());
        out.put("idNo", maskIdNo(q.getIdNo()));
        out.put("idFrontFile", q.getIdFrontFile());
        out.put("idBackFile", q.getIdBackFile());
        out.put("licenseFile", q.getLicenseFile());
        out.put("faceStatus", q.getFaceStatus());
        out.put("faceProvider", q.getFaceProvider());
        out.put("faceRef", q.getFaceRef());
        out.put("faceTime", q.getFaceTime());
        out.put("reviewStatus", q.getReviewStatus());
        out.put("reviewRemark", q.getReviewRemark());
        out.put("submitTime", q.getSubmitTime());
        out.put("reviewTime", q.getReviewTime());
        return out;
    }

    /** 师傅提交/重新提交资质 */
    @Transactional(rollbackFor = Exception.class)
    public void submit(Long workerId, String idName, String idNo, String idFrontFile,
                       String idBackFile, String licenseFile) {
        if (idName == null || idName.isBlank()) {
            throw new ApiException(ErrorCode.A0001, "请填写身份证姓名");
        }
        if (idNo == null || idNo.isBlank()) {
            throw new ApiException(ErrorCode.A0001, "请填写身份证号");
        }
        if (idFrontFile == null || idFrontFile.isBlank() || idBackFile == null || idBackFile.isBlank()) {
            throw new ApiException(ErrorCode.A0001, "请上传身份证正反面照片");
        }
        WashWorkerQualification q = qualificationMapper.selectByWorkerId(workerId);
        long now = System.currentTimeMillis();
        if (q == null) {
            q = new WashWorkerQualification();
            q.setWorkerId(workerId);
            q.setIdName(idName.trim());
            q.setIdNo(idNo.trim());
            q.setIdFrontFile(idFrontFile);
            q.setIdBackFile(idBackFile);
            q.setLicenseFile(licenseFile);
            q.setReviewStatus("PENDING");
            q.setSubmitTime(now);
            qualificationMapper.insert(q);
            return;
        }
        // 已提交过：覆盖证件信息并重新送审（人脸核身结果保留）
        q.setIdName(idName.trim());
        q.setIdNo(idNo.trim());
        q.setIdFrontFile(idFrontFile);
        q.setIdBackFile(idBackFile);
        q.setLicenseFile(licenseFile);
        q.setSubmitTime(now);
        qualificationMapper.resubmit(q);
    }

    /** 后台待审核分页 */
    public Map<String, Object> adminPage(String reviewStatus, int pageNum, int pageSize) {
        if (pageNum < 1) pageNum = 1;
        if (pageSize < 1 || pageSize > 100) pageSize = 10;
        int offset = (pageNum - 1) * pageSize;
        List<WashWorkerQualification> rows =
                qualificationMapper.selectByReviewStatus(reviewStatus, offset, pageSize);
        Map<String, Object> data = new HashMap<>();
        data.put("rows", rows);
        data.put("total", qualificationMapper.countByReviewStatus(reviewStatus));
        return data;
    }

    /** 后台审核：PASSED / REJECTED（驳回必须带原因） */
    @Transactional(rollbackFor = Exception.class)
    public void review(Long workerId, String reviewStatus, String reviewRemark, String reviewer) {
        if (!"PASSED".equals(reviewStatus) && !"REJECTED".equals(reviewStatus)) {
            throw new ApiException(ErrorCode.A0001, "审核结果只支持 PASSED / REJECTED");
        }
        if ("REJECTED".equals(reviewStatus) && (reviewRemark == null || reviewRemark.isBlank())) {
            throw new ApiException(ErrorCode.A0001, "驳回必须填写原因，师傅要看到为什么没过");
        }
        WashWorkerQualification q = qualificationMapper.selectByWorkerId(workerId);
        if (q == null) {
            throw new ApiException(ErrorCode.A0001, "该师傅尚未提交资质");
        }
        qualificationMapper.review(workerId, reviewStatus, reviewRemark, reviewer, System.currentTimeMillis());
    }

    /**
     * 人脸核身结果回填。
     *
     * @param faceStatus  PASSED / FAILED
     * @param faceProvider WECHAT（微信核身回调）/ MANUAL（后台人工登记）
     * @param faceRef      核身凭证号
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateFace(Long workerId, String faceStatus, String faceProvider, String faceRef) {
        if (!"PASSED".equals(faceStatus) && !"FAILED".equals(faceStatus)) {
            throw new ApiException(ErrorCode.A0001, "核身结果只支持 PASSED / FAILED");
        }
        if (qualificationMapper.updateFace(workerId, faceStatus, faceProvider, faceRef,
                System.currentTimeMillis()) == 0) {
            throw new ApiException(ErrorCode.A0001, "该师傅尚未提交资质，无法登记核身结果");
        }
    }

    /** 资质是否全部通过（派单准入） */
    public boolean isPassed(Long workerId) {
        WashWorkerQualification q = qualificationMapper.selectByWorkerId(workerId);
        return q != null && "PASSED".equals(q.getFaceStatus()) && "PASSED".equals(q.getReviewStatus());
    }

    private String maskIdNo(String idNo) {
        if (idNo == null || idNo.length() < 8) {
            return idNo;
        }
        return idNo.substring(0, 4) + "**********" + idNo.substring(idNo.length() - 4);
    }
}
