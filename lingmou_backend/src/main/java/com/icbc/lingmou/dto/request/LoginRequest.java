package com.icbc.lingmou.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 登录请求DTO
 * 人员A写的示例，给其他人参考格式
 */
@Data
@Schema(description = "登录请求参数")
public class LoginRequest {

    @Schema(description = "用户名/手机号", example = "13800138000")
    @NotBlank(message = "用户名不能为空")
    private String username;

    @Schema(description = "密码", example = "123456")
    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 20, message = "密码长度6-20位")
    private String password;
}
