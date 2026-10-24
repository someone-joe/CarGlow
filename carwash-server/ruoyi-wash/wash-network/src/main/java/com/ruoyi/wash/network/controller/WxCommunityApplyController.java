package com.ruoyi.wash.network.controller;

import com.ruoyi.wash.common.api.ApiException;
import com.ruoyi.wash.common.api.ApiResult;
import com.ruoyi.wash.common.api.ErrorCode;
import com.ruoyi.wash.common.security.MemberContext;
import com.ruoyi.wash.network.service.WashCommunityApplyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * C 端小区开通申请。契约：openapi.yaml POST /api/v1/community-applies（tag site）。
 * memberId 取自 MemberContext（会员拦截器已写入），不接收前端传入，防越权枚举。
 */
@RestController
public class WxCommunityApplyController {

    @Autowired
    private WashCommunityApplyService applyService;

    @PostMapping("/api/v1/community-applies")
    public ApiResult<Long> submit(@RequestBody(required = false) CommunityApplySubmitRequest req) {
        if (req == null || req.communityName() == null || req.communityName().isBlank()) {
            throw new ApiException(ErrorCode.A0001, "小区名称必填");
        }
        Long applyId = applyService.submit(MemberContext.require(), req.communityName(), req.contactPhone());
        return ApiResult.ok(applyId);
    }

    public record CommunityApplySubmitRequest(String communityName, String contactPhone) {
    }
}
