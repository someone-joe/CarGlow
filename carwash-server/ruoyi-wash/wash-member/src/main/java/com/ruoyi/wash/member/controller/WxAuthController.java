package com.ruoyi.wash.member.controller;

import com.ruoyi.wash.common.api.ApiException;
import com.ruoyi.wash.common.api.ApiResult;
import com.ruoyi.wash.common.api.ErrorCode;
import com.ruoyi.wash.member.dto.AgreementVO;
import com.ruoyi.wash.member.dto.LoginVO;
import com.ruoyi.wash.member.service.WashAgreementService;
import com.ruoyi.wash.member.service.WashAuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * C 端登录接口。契约：openapi.yaml /api/v1/auth/login。
 */
@RestController
public class WxAuthController {

    @Autowired
    private WashAuthService authService;

    @Autowired
    private WashAgreementService agreementService;

    @PostMapping("/api/v1/auth/login")
    public ApiResult<LoginVO> login(@RequestBody LoginRequest request) {
        if (request == null || request.code() == null || request.code().isBlank()) {
            throw new ApiException(ErrorCode.A0001, "code 必填");
        }
        return ApiResult.ok(authService.login(request.code(), request.phoneCode()));
    }

    /**
     * 协议富文本。契约：openapi.yaml GET /api/v1/auth/agreements（security: []，免登录）。
     * /api/v1/auth/** 已在 MemberWebConfig 排除拦截，无需额外登记。
     */
    @GetMapping("/api/v1/auth/agreements")
    public ApiResult<AgreementVO> agreements(@RequestParam String type) {
        return ApiResult.ok(agreementService.get(type));
    }

    /** 与契约 request body 逐字段一致。 */
    public record LoginRequest(String code, String phoneCode) {
    }
}
