package com.ruoyi.wash.member.controller;

import com.ruoyi.wash.common.api.ApiException;
import com.ruoyi.wash.common.api.ApiResult;
import com.ruoyi.wash.common.api.ErrorCode;
import com.ruoyi.wash.member.dto.LoginVO;
import com.ruoyi.wash.member.service.WashAuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * C 端登录接口。契约：openapi.yaml /api/v1/auth/login。
 */
@RestController
public class WxAuthController {

    @Autowired
    private WashAuthService authService;

    @PostMapping("/api/v1/auth/login")
    public ApiResult<LoginVO> login(@RequestBody LoginRequest request) {
        if (request == null || request.code() == null || request.code().isBlank()) {
            throw new ApiException(ErrorCode.A0001, "code 必填");
        }
        return ApiResult.ok(authService.login(request.code()));
    }

    /** 与契约 request body 逐字段一致。 */
    public record LoginRequest(String code, String phoneCode) {
    }
}
