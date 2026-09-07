package com.icbc.lingmou.config;

import com.icbc.lingmou.interceptor.AuthInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web配置
 * 注册登录拦截器，配置不需要登录的接口
 */
@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final AuthInterceptor authInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/api/**")        // 拦截所有/api开头的请求
                .excludePathPatterns(             // 以下接口不需要登录
                        "/api/auth/login",          // 登录
                        "/api/auth/register",       // 注册
                        "/api/auth/sms-code",       // 发送验证码
                        "/api/auth/reset-password", // 找回密码
                        "/api/branches",            // 网点列表（公开）
                        "/api/branches/*",          // 网点详情（公开）
                        "/api/branches/*/heatmap", // 网点热力图（公开）
                        "/api/test/**",             // 测试接口
                        "/swagger/**",              // Swagger文档
                        "/v3/api-docs/**",          // OpenAPI
                        "/webjars/**"               // 静态资源
                );
    }
}
