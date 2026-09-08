package com.icbc.lingmou.config;

import com.icbc.lingmou.interceptor.AuthInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web 配置
 * - 注册登录拦截器，配置不需要登录的接口
 * - 配置本地文件上传目录的静态资源映射：/uploads/** → file:{uploadDir}/
 */
@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final AuthInterceptor authInterceptor;

    @Value("${lingmou.upload.dir:./uploads}")
    private String uploadDir;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/api/**")        // 拦截所有 /api 开头的请求
                .excludePathPatterns(             // 以下接口不需要登录
                        "/api/auth/login",          // 登录
                        "/api/auth/register",       // 注册
                        "/api/auth/sms-code",       // 发送验证码
                        "/api/auth/reset-password", // 找回密码
                        "/api/branches",            // 网点列表（公开）
                        "/api/branches/*",          // 网点详情（公开）
                        "/api/branches/*/heatmap",  // 网点热力图（公开）
                        "/api/vouchers/*/validate", // 凭证号校验（公开）
                        "/api/vouchers/rule",       // 凭证号规则（公开）
                        "/api/qrcode/*/image",      // 业务直通码二维码图片（公开，扫码侧展示用）
                        "/api/test/**",             // 测试接口
                        "/swagger/**",              // Swagger 文档
                        "/swagger-ui/**",
                        "/swagger-ui.html",
                        "/v3/api-docs/**",          // OpenAPI
                        "/webjars/**",              // 静态资源
                        "/uploads/**"               // 上传文件静态资源
                );
    }

    /**
     * 本地文件上传目录静态资源映射
     * /uploads/** → file:{uploadDir}/
     * 开发模式下 Vite proxy 会把 /uploads/** 也代理到 8080（同域名）
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String location = uploadDir.endsWith("/") ? uploadDir : uploadDir + "/";
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + location);
    }
}
