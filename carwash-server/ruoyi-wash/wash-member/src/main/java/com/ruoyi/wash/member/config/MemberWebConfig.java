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
                .excludePathPatterns("/api/v1/auth/**", "/api/v1/payments/wechat/**");
    }
}
