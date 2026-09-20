package com.ruoyi.wash.worker.controller;

import com.ruoyi.wash.common.api.ApiException;
import com.ruoyi.wash.common.api.ApiResult;
import com.ruoyi.wash.common.api.ErrorCode;
import com.ruoyi.wash.common.security.WorkerContext;
import com.ruoyi.wash.worker.dto.StationQueueVO;
import com.ruoyi.wash.worker.service.WashStationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 师傅端作业接口。契约：openapi.yaml /api/v1/station/**。
 * 已登录师傅身份由 WorkerAuthInterceptor 写入 WorkerContext，这里直接取，不自己解析 token。
 */
@RestController
public class WashStationController {

    @Autowired
    private WashStationService stationService;

    @GetMapping("/api/v1/station/queue")
    public ApiResult<StationQueueVO> queue() {
        return ApiResult.ok(stationService.queue(WorkerContext.require()));
    }

    @PostMapping("/api/v1/station/orders/{orderNo}/arrive")
    public ApiResult<Void> arrive(@PathVariable String orderNo) {
        stationService.arrive(orderNo, WorkerContext.require());
        return ApiResult.<Void>ok(null);
    }

    @PostMapping("/api/v1/station/orders/{orderNo}/leave")
    public ApiResult<Void> leave(@PathVariable String orderNo) {
        stationService.leave(orderNo, WorkerContext.require());
        return ApiResult.<Void>ok(null);
    }

    @PostMapping("/api/v1/station/orders/{orderNo}/sop-done")
    public ApiResult<Void> sopDone(@PathVariable String orderNo, @RequestBody SopDoneRequest request) {
        if (request == null) {
            throw new ApiException(ErrorCode.A0001, "请求体必填");
        }
        stationService.sopDone(orderNo, request.steps(), WorkerContext.require());
        return ApiResult.<Void>ok(null);
    }

    @PostMapping("/api/v1/station/orders/{orderNo}/qc-pass")
    public ApiResult<Void> qcPass(@PathVariable String orderNo, @RequestBody QcPassRequest request) {
        if (request == null) {
            throw new ApiException(ErrorCode.A0001, "请求体必填");
        }
        stationService.qcPass(orderNo, request.fileIds(), WorkerContext.require());
        return ApiResult.<Void>ok(null);
    }

    @PostMapping("/api/v1/station/orders/{orderNo}/qc-fail")
    public ApiResult<Void> qcFail(@PathVariable String orderNo, @RequestBody QcFailRequest request) {
        if (request == null) {
            throw new ApiException(ErrorCode.A0001, "请求体必填");
        }
        stationService.qcFail(orderNo, request.reason(), WorkerContext.require());
        return ApiResult.<Void>ok(null);
    }

    /** 与契约 request body 逐字段一致。 */
    public record SopDoneRequest(List<String> steps) {
    }

    /** 与契约 request body 逐字段一致。 */
    public record QcPassRequest(List<String> fileIds) {
    }

    /** 与契约 request body 逐字段一致。 */
    public record QcFailRequest(String reason) {
    }
}
