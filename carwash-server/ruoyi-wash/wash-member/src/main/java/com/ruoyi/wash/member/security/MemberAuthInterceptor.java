package com.ruoyi.wash.member.security;

import com.alibaba.fastjson2.JSON;
import com.ruoyi.common.utils.ServletUtils;
import com.ruoyi.wash.common.api.ApiResult;
import com.ruoyi.wash.common.api.ErrorCode;
import com.ruoyi.wash.common.security.MemberContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * C 端接口鉴权拦截器：校验 token → 写入 MemberContext → 请求结束清理。
 * 注意：只拦 /api/v1/**（见 MemberWebConfig），后台 /admin-api/** 与若依体系互不干扰。
 */
@Component
public class MemberAuthInterceptor implements HandlerInterceptor {

    @Autowired
    private MemberTokenService tokenService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 预检请求直接放行
        if (HttpMethod.OPTIONS.matches(request.getMethod())) {
            return true;
        }
        Long memberId = tokenService.getMemberId(tokenService.resolveToken(request.getHeader(MemberTokenService.HEADER)));
        if (memberId == null) {
            // HTTP 仍返回 200，错误通过契约错误码 A0002 表达，前端据此跳登录
            ServletUtils.renderString(response, JSON.toJSONString(ApiResult.fail(ErrorCode.A0002)));
            return false;
        }
        MemberContext.set(memberId);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        MemberContext.clear();
    }
}
