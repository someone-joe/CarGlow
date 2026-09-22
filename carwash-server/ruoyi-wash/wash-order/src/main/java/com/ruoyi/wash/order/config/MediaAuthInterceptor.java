package com.ruoyi.wash.order.config;

import com.alibaba.fastjson2.JSON;
import com.ruoyi.common.utils.ServletUtils;
import com.ruoyi.framework.web.service.TokenService;
import com.ruoyi.wash.common.api.ApiResult;
import com.ruoyi.wash.common.api.ErrorCode;
import com.ruoyi.wash.common.security.MemberContext;
import com.ruoyi.wash.member.security.MemberTokenService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 影像读取鉴权：两类令牌、两种携带方式。
 *
 * <p>为什么需要 query token：小程序 image / previewImage 与后台 el-image 都无法自定义请求头，
 * 不支持 query token 的话所有影像缩略图与大图都加载不出来（bug092201）。
 * token 进 URL 有被日志记录的风险，只用于图片读取这个低敏场景。
 *
 * <p>令牌优先级：C 端会员 token（会写 MemberContext，upload 接口依赖）→
 * 后台若依 token（仅放行 GET：后台只读影像不写入）。后台 token 用 request 包装器
 * 复用若依 getLoginUser(request)，不改若依任何代码。
 */
@Component
public class MediaAuthInterceptor implements HandlerInterceptor {

    public static final String QUERY_TOKEN = "token";

    @Autowired
    private MemberTokenService memberTokenService;

    @Autowired
    private TokenService adminTokenService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (HttpMethod.OPTIONS.matches(request.getMethod())) {
            return true;
        }
        String headerToken = memberTokenService.resolveToken(request.getHeader(MemberTokenService.HEADER));
        String queryToken = request.getParameter(QUERY_TOKEN);

        // 1) C 端会员 token（header 或 query）
        Long memberId = resolveMember(headerToken);
        if (memberId == null) {
            memberId = resolveMember(queryToken);
        }
        if (memberId != null) {
            // upload 接口要用 MemberContext.require() 取上传人，所以这里负责写入上下文
            MemberContext.set(memberId);
            return true;
        }

        // 2) 后台若依 token：只放行读取（GET）
        String adminToken = headerToken != null ? headerToken : queryToken;
        if (adminToken != null && !adminToken.isBlank()
                && HttpMethod.GET.matches(request.getMethod())
                && isAdminLogin(adminToken, request)) {
            return true;
        }

        ServletUtils.renderString(response, JSON.toJSONString(ApiResult.fail(ErrorCode.A0002)));
        return false;
    }

    private Long resolveMember(String token) {
        return token == null || token.isBlank() ? null : memberTokenService.getMemberId(token);
    }

    /** 用包装器把裸 token 塞进 Authorization，复用若依 getLoginUser(request)，不改若依代码 */
    private boolean isAdminLogin(String token, HttpServletRequest request) {
        HttpServletRequest carrier = new HttpServletRequestWrapper(request) {
            @Override
            public String getHeader(String name) {
                if (MemberTokenService.HEADER.equalsIgnoreCase(name)) {
                    return "Bearer " + token;
                }
                return super.getHeader(name);
            }
        };
        return adminTokenService.getLoginUser(carrier) != null;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        MemberContext.clear();
    }
}
