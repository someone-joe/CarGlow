package com.ruoyi.wash.order.config;

import com.alibaba.fastjson2.JSON;
import com.ruoyi.common.utils.ServletUtils;
import com.ruoyi.wash.common.api.ApiResult;
import com.ruoyi.wash.common.api.ErrorCode;
import com.ruoyi.wash.common.security.MemberContext;
import com.ruoyi.wash.member.security.MemberTokenService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 影像读取鉴权：token 可走 header，也可走 query。
 *
 * <p>为什么需要 query：小程序的 image / previewImage 组件发请求时无法自定义 header，
 * 不支持 query token 的话，所有影像缩略图与大图预览都加载不出来（bug092201）。
 * token 进 URL 有被日志记录的风险，故只用于图片读取这个低敏场景，其余接口一律 header。
 */
@Component
public class MediaAuthInterceptor implements HandlerInterceptor {

    public static final String QUERY_TOKEN = "token";

    @Autowired
    private MemberTokenService tokenService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (HttpMethod.OPTIONS.matches(request.getMethod())) {
            return true;
        }
        String headerToken = tokenService.resolveToken(request.getHeader(MemberTokenService.HEADER));
        String token = headerToken != null ? headerToken : request.getParameter(QUERY_TOKEN);
        Long memberId = token == null || token.isBlank() ? null : tokenService.getMemberId(token);
        if (memberId == null) {
            // HTTP 仍返回 200，错误通过契约错误码 A0002 表达
            ServletUtils.renderString(response, JSON.toJSONString(ApiResult.fail(ErrorCode.A0002)));
            return false;
        }
        // upload 接口要用 MemberContext.require() 取上传人，所以本拦截器必须负责写入上下文
        MemberContext.set(memberId);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        MemberContext.clear();
    }
}
