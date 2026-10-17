package com.ruoyi.wash.worker.controller;

import com.ruoyi.wash.common.api.ApiException;
import com.ruoyi.wash.common.api.ApiResult;
import com.ruoyi.wash.common.api.ErrorCode;
import com.ruoyi.wash.common.security.WorkerContext;
import com.ruoyi.wash.order.dto.MediaVO;
import com.ruoyi.wash.order.service.WashMediaService;
import com.ruoyi.wash.worker.domain.WashWorker;
import com.ruoyi.wash.worker.service.WashDispatchService;
import com.ruoyi.wash.worker.service.WashWorkerQualificationService;
import com.ruoyi.wash.worker.service.WashWorkerScheduleService;
import com.ruoyi.wash.worker.service.WashWorkerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 师傅端「我的」：资料 / 打卡上下班 / 资质提交与查看。契约：openapi.yaml /api/v1/worker/**。
 * 已登录师傅身份由 WorkerAuthInterceptor 写入 WorkerContext。
 */
@RestController
@RequestMapping("/api/v1/worker")
public class WxWorkerController {

    @Autowired
    private WashWorkerService workerService;

    @Autowired
    private WashWorkerQualificationService qualificationService;

    @Autowired
    private WashWorkerScheduleService scheduleService;

    @Autowired
    private WashDispatchService dispatchService;

    @Autowired
    private WashMediaService mediaService;

    /** 我的资料 + 状态：上下班、资质是否通过、今天是否在班、进行中订单数 */
    @GetMapping("/profile")
    public ApiResult<Map<String, Object>> profile() {
        Long workerId = WorkerContext.require();
        WashWorker worker = workerService.require(workerId);
        Map<String, Object> out = new HashMap<>();
        out.put("workerId", worker.getWorkerId());
        out.put("workerNo", worker.getWorkerNo());
        out.put("name", worker.getName());
        out.put("phone", worker.getPhone());
        out.put("siteId", worker.getSiteId());
        out.put("communityId", worker.getCommunityId());
        out.put("workStatus", worker.getWorkStatus());
        out.put("qualificationPassed", qualificationService.isPassed(workerId));
        out.put("onDutyToday", scheduleService.isOnDutyToday(workerId));
        out.put("activeOrderCount", dispatchService.activeCount(workerId));
        return ApiResult.ok(out);
    }

    /** 打卡上下班：OFFLINE / ONLINE / BUSY */
    @PostMapping("/work-status")
    public ApiResult<Map<String, Object>> workStatus(@RequestBody(required = false) WorkStatusRequest request) {
        if (request == null || request.workStatus() == null || request.workStatus().isBlank()) {
            throw new ApiException(ErrorCode.A0001, "workStatus 必填（OFFLINE / ONLINE / BUSY）");
        }
        Long workerId = WorkerContext.require();
        String next = workerService.toggleWork(workerId, request.workStatus());
        Map<String, Object> out = new HashMap<>();
        out.put("workStatus", next);
        return ApiResult.ok(out);
    }

    /** 我的资质（未提交时返回空态） */
    @GetMapping("/qualification")
    public ApiResult<Map<String, Object>> qualification() {
        return ApiResult.ok(qualificationService.my(WorkerContext.require()));
    }

    /** 提交 / 重新提交资质（身份证 + 驾照） */
    @PostMapping("/qualification")
    public ApiResult<Void> submitQualification(@RequestBody(required = false) QualificationRequest request) {
        if (request == null) {
            throw new ApiException(ErrorCode.A0001, "请求体必填");
        }
        qualificationService.submit(WorkerContext.require(), request.idName(), request.idNo(),
                request.idFrontFile(), request.idBackFile(), request.licenseFile());
        return ApiResult.<Void>ok(null);
    }

    /** 与契约 request body 逐字段一致。 */
    public record WorkStatusRequest(String workStatus) {
    }

    public record QualificationRequest(String idName, String idNo, String idFrontFile,
                                       String idBackFile, String licenseFile) {
    }

    // ---------------- 影像（师傅资质照片，独立端点，走 worker 鉴权） ----------------

    /** 师傅资质照片上传：走 worker 身份，bizType 固定 WORKER_QUAL，不挂订单 */
    @PostMapping("/media/upload")
    public ApiResult<MediaVO> uploadMedia(@RequestParam("file") org.springframework.web.multipart.MultipartFile file,
                                         @RequestParam("bizType") String bizType) {
        Long workerId = WorkerContext.require();
        return ApiResult.ok(mediaService.upload(file, bizType, null, null, "WORKER:" + workerId));
    }

    /** 师傅读取已上传的资质照片（worker 鉴权，不走会员/后台拦截器） */
    @GetMapping("/media/{fileId}/raw")
    public org.springframework.http.ResponseEntity<org.springframework.core.io.Resource> mediaRaw(@PathVariable String fileId) {
        java.nio.file.Path path = mediaService.resolve(fileId);
        if (path == null || !java.nio.file.Files.exists(path)) {
            return org.springframework.http.ResponseEntity.notFound().build();
        }
        org.springframework.http.MediaType type = org.springframework.http.MediaTypeFactory
                .getMediaType(path.getFileName().toString())
                .orElse(org.springframework.http.MediaType.APPLICATION_OCTET_STREAM);
        return org.springframework.http.ResponseEntity.ok().contentType(type)
                .body(new org.springframework.core.io.FileSystemResource(path.toFile()));
    }
}
