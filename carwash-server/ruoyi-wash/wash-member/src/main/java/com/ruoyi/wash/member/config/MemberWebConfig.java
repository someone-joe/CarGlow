package com.ruoyi.wash.member.config;

import com.ruoyi.wash.member.security.MemberAuthInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * C 端拦截器注册。登录相关接口匿名，其余 /api/v1/** 必须带会员 token。
 * 新增免登录接口必须在此登记，并说明理由。
 */
@Configuration
public class MemberWebConfig implements WebMvcConfigurer {

    @Autowired
    private MemberAuthInterceptor memberAuthInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(memberAuthInterceptor)
                .addPathPatterns("/api/v1/**")
                // 登录接口与支付回调不需要会员 token（回调是微信服务器调用，无登录态）
                // 师傅端路径（worker/pick/station）交给 wash-worker 的 WorkerAuthInterceptor，
                // C 端拦截器必须放行，否则师傅端请求会被 C 端 token 校验拦下返回 A0002
                .excludePathPatterns("/api/v1/auth/**", "/api/v1/payments/wechat/**",
                        "/api/v1/worker/**", "/api/v1/pick/**", "/api/v1/station/**",
                        // 影像读取由 MediaAuthInterceptor 接管（支持 query token，供 image 组件使用）
                        "/api/v1/media/**");
    }
}
