package com.icbc.lingmou.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 认证响应DTO（登录/注册后返回Token和用户信息）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "认证响应")
public class AuthResponse {

    @Schema(description = "JWT Token")
    private String token;

    @Schema(description = "Token过期时间（秒）")
    private Long expiresIn;

    @Schema(description = "用户信息")
    private UserInfoResponse userInfo;
}
