package com.ruoyi.wash.worker.controller;

import com.ruoyi.wash.common.api.ApiException;
import com.ruoyi.wash.common.api.ApiResult;
import com.ruoyi.wash.common.api.ErrorCode;
import com.ruoyi.wash.worker.dto.WorkerLoginVO;
import com.ruoyi.wash.worker.service.WashWorkerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * 师傅端登录接口。契约：openapi.yaml /api/v1/worker/login。
 * 师傅与顾客共用同一小程序，但登录态独立：这里是工号登录，不是微信登录。
 */
@RestController
public class WorkerLoginController {

    @Autowired
    private WashWorkerService workerService;

    @PostMapping("/api/v1/worker/login")
    public ApiResult<WorkerLoginVO> login(@RequestBody LoginRequest request) {
        if (request == null || request.workerNo() == null || request.workerNo().isBlank()) {
            throw new ApiException(ErrorCode.A0001, "工号必填");
        }
        if (request.password() == null || request.password().isBlank()) {
            throw new ApiException(ErrorCode.A0001, "密码必填");
        }
        return ApiResult.ok(workerService.login(request.workerNo(), request.password()));
    }

    /** 与契约 request body 逐字段一致。 */
    public record LoginRequest(String workerNo, String password) {
    }
}
