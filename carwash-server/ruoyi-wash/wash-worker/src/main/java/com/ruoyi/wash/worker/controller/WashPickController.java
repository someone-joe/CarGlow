package com.ruoyi.wash.worker.controller;

import com.ruoyi.wash.common.api.ApiException;
import com.ruoyi.wash.common.api.ApiResult;
import com.ruoyi.wash.common.api.ErrorCode;
import com.ruoyi.wash.common.security.WorkerContext;
import com.ruoyi.wash.worker.dto.PickTaskVO;
import com.ruoyi.wash.worker.service.WashPickService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 师傅端取送接口。契约：openapi.yaml /api/v1/pick/**。
 * 已登录师傅身份由 WorkerAuthInterceptor 写入 WorkerContext，这里直接取，不自己解析 token。
 */
@RestController
public class WashPickController {

    @Autowired
    private WashPickService pickService;

    @GetMapping("/api/v1/pick/tasks")
    public ApiResult<List<PickTaskVO>> tasks(@RequestParam(required = false) String stage) {
        return ApiResult.ok(pickService.tasks(WorkerContext.require(), stage));
    }

    @PostMapping("/api/v1/pick/orders/{orderNo}/take-key")
    public ApiResult<Void> takeKey(@PathVariable String orderNo) {
        pickService.takeKey(orderNo, WorkerContext.require());
        return ApiResult.<Void>ok(null);
    }

    @PostMapping("/api/v1/pick/orders/{orderNo}/pick-car-done")
    public ApiResult<Void> pickCarDone(@PathVariable String orderNo, @RequestBody PickCarDoneRequest request) {
        if (request == null) {
            throw new ApiException(ErrorCode.A0001, "请求体必填");
        }
        pickService.pickCarDone(orderNo, request.fileIds(), request.note(), WorkerContext.require());
        return ApiResult.<Void>ok(null);
    }

    @PostMapping("/api/v1/pick/orders/{orderNo}/return-done")
    public ApiResult<Void> returnDone(@PathVariable String orderNo, @RequestBody ReturnDoneRequest request) {
        if (request == null) {
            throw new ApiException(ErrorCode.A0001, "请求体必填");
        }
        pickService.returnDone(orderNo, request.fileIds(), request.parkingNo(), WorkerContext.require());
        return ApiResult.<Void>ok(null);
    }

    /** 与契约 request body 逐字段一致。 */
    public record PickCarDoneRequest(List<String> fileIds, String note) {
    }

    /** 与契约 request body 逐字段一致。 */
    public record ReturnDoneRequest(List<String> fileIds, String parkingNo) {
    }
}
