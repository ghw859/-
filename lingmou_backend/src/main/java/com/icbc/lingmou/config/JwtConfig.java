package com.icbc.lingmou.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * JWT配置属性
 * 从application.yml中读取jwt.*配置
 */
@Data
@Component
@ConfigurationProperties(prefix = "jwt")
public class JwtConfig {

    /** JWT密钥 */
    private String secret;

    /** 过期时间（毫秒） */
    private Long expiration;

    /** 请求头名称 */
    private String header;

    /** Token前缀 */
    private String tokenPrefix;
}
