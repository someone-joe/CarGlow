package com.ruoyi.wash.worker.security;

import com.alibaba.fastjson2.JSON;
import com.ruoyi.common.utils.ServletUtils;
import com.ruoyi.wash.common.api.ApiResult;
import com.ruoyi.wash.common.api.ErrorCode;
import com.ruoyi.wash.common.security.WorkerContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 师傅端接口鉴权拦截器：校验 worker token → 写入 WorkerContext → 请求结束清理。
 * 只拦 /api/v1/worker/**、/api/v1/pick/**、/api/v1/station/**（见 WorkerWebConfig），
 * 且这些路径已由 MemberWebConfig 放行，C 端拦截器不会介入。
 */
@Component
public class WorkerAuthInterceptor implements HandlerInterceptor {

    @Autowired
    private WorkerTokenService tokenService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (HttpMethod.OPTIONS.matches(request.getMethod())) {
            return true;
        }
        Long workerId = tokenService.getWorkerId(tokenService.resolveToken(request.getHeader(WorkerTokenService.HEADER)));
        if (workerId == null) {
            // HTTP 仍返回 200，错误通过契约错误码 A0002 表达，前端据此跳工号登录页
            ServletUtils.renderString(response, JSON.toJSONString(ApiResult.fail(ErrorCode.A0002)));
            return false;
        }
        WorkerContext.set(workerId);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        WorkerContext.clear();
    }
}
