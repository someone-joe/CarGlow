package com.ruoyi.wash.order.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 影像接口拦截注册。/api/v1/media/** 已由 MemberWebConfig 放行，改由本拦截器接管：
 * 支持 header 与 query 两种 token 携带方式（原因见 MediaAuthInterceptor）。
 */
@Configuration
public class MediaWebConfig implements WebMvcConfigurer {

    @Autowired
    private MediaAuthInterceptor mediaAuthInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(mediaAuthInterceptor)
                .addPathPatterns("/api/v1/media/**");
    }
}
