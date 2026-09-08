package com.icbc.lingmou.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * SpringDoc 接口文档分组配置
 * 访问地址: http://localhost:8080/swagger-ui.html
 */
@Configuration
public class SwaggerConfig {

    /**
     * 全局 OpenAPI 元信息
     */
    @Bean
    public OpenAPI lingmouOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("ICBC · 灵枢 后端 API")
                        .description("智能网点业务直通平台 - Java 后端接口文档\n\n"
                                + "**错误码段位**：用户 1xxxx / 预约 2xxxx / 网点 3xxxx / 预填单 4xxxx / 审计 5xxxx / AI 6xxxx / 系统 9xxxx\n\n"
                                + "**端口**：Java 8080 · Python 8000 · 前端 5173")
                        .version("1.0.0")
                        .contact(new Contact().name("灵枢开发组").email("dev@lingmou.icbc"))
                        .license(new License().name("MIT").url("https://opensource.org/licenses/MIT"))
                );
    }

    /**
     * 分组：认证
     */
    @Bean
    public GroupedOpenApi authApi() {
        return GroupedOpenApi.builder()
                .group("1. 认证管理")
                .pathsToMatch("/api/auth/**")
                .build();
    }

    /**
     * 分组：网点
     */
    @Bean
    public GroupedOpenApi branchApi() {
        return GroupedOpenApi.builder()
                .group("2. 网点管理")
                .pathsToMatch("/api/branches/**")
                .build();
    }

    /**
     * 分组：预约
     */
    @Bean
    public GroupedOpenApi appointmentApi() {
        return GroupedOpenApi.builder()
                .group("3. 预约管理")
                .pathsToMatch("/api/appointments/**")
                .build();
    }

    /**
     * 分组：凭证/二维码
     */
    @Bean
    public GroupedOpenApi voucherApi() {
        return GroupedOpenApi.builder()
                .group("4. 凭证与业务直通码")
                .pathsToMatch("/api/vouchers/**", "/api/qrcode/**")
                .build();
    }

    /**
     * 分组：预填单
     */
    @Bean
    public GroupedOpenApi preformApi() {
        return GroupedOpenApi.builder()
                .group("5. 预填单")
                .pathsToMatch("/api/preforms/**")
                .build();
    }

    /**
     * 分组：AI 相关
     */
    @Bean
    public GroupedOpenApi aiApi() {
        return GroupedOpenApi.builder()
                .group("6. AI 预检（代理 Python 8000）")
                .pathsToMatch("/api/ai/**")
                .build();
    }

    /**
     * 分组：测试
     */
    @Bean
    public GroupedOpenApi testApi() {
        return GroupedOpenApi.builder()
                .group("99. 测试接口")
                .pathsToMatch("/api/test/**")
                .build();
    }
}
