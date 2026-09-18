package com.ruoyi.wash.member.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * C 端接口（/api/v1/**）的独立安全链。
 *
 * <p>若依默认「除白名单外全部要后台 JWT 鉴权」，会直接 401 拦死小程序接口。
 * 这里叠加一条只匹配 /api/v1/** 的过滤链，放行进入后由 MemberAuthInterceptor
 * 用会员 token 做鉴权 —— 不改动若依任何一行代码。
 *
 * <p>注意：@Order 必须标在 @Bean 方法上（标在配置类上不参与链排序），
 * 且值必须小于若依的"匹配所有请求"链，否则 Spring Security 启动即报 UnreachableFilterChainException。
 */
@Configuration
public class WxApiSecurityConfig {

    @Bean
    @Order(1)
    public SecurityFilterChain wxApiFilterChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher("/api/v1/**", "/device-callback/**")
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        return http.build();
    }
}
