package com.ruoyi.wash.worker.config;

import com.ruoyi.wash.worker.security.WorkerAuthInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 师傅端拦截器注册。工号登录接口匿名，其余师傅端接口必须带 worker token。
 * 新增免登录的师傅端接口必须在此登记，并说明理由。
 */
@Configuration
public class WorkerWebConfig implements WebMvcConfigurer {

    @Autowired
    private WorkerAuthInterceptor workerAuthInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(workerAuthInterceptor)
                .addPathPatterns("/api/v1/worker/**", "/api/v1/pick/**", "/api/v1/station/**")
                .excludePathPatterns("/api/v1/worker/login");
    }
}
