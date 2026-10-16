package com.ruoyi.wash.promo.controller;

import com.ruoyi.wash.common.api.ApiException;
import com.ruoyi.wash.common.api.ApiResult;
import com.ruoyi.wash.common.api.ErrorCode;
import com.ruoyi.wash.common.security.MemberContext;
import com.ruoyi.wash.promo.dto.InviteListVO;
import com.ruoyi.wash.promo.service.WashInviteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * C 端邀请好友。契约：openapi.yaml /api/v1/invite/**。
 *
 * <p>发奖不在本控制器触发：由订单支付成功处调用 WashInviteService.onFirstPaidOrder。
 */
@RestController
public class WxInviteController {

    @Autowired
    private WashInviteService inviteService;

    /** 我的邀请码（首次调用生成） */
    @GetMapping("/api/v1/invite/code")
    public ApiResult<String> code() {
        return ApiResult.ok(inviteService.myCode(MemberContext.require()));
    }

    /** 绑定邀请码 */
    @PostMapping("/api/v1/invite/bind")
    public ApiResult<Void> bind(@RequestBody(required = false) Map<String, String> body) {
        String code = body == null ? null : body.get("code");
        if (code == null || code.isBlank()) {
            throw new ApiException(ErrorCode.A0001, "请填写邀请码");
        }
        inviteService.bind(MemberContext.require(), code);
        return ApiResult.ok();
    }

    /** 我邀请的人 + 统计 */
    @GetMapping("/api/v1/invite/list")
    public ApiResult<InviteListVO> list() {
        return ApiResult.ok(inviteService.myInvites(MemberContext.require()));
    }
}
