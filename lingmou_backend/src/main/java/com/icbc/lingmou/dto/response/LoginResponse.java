package com.icbc.lingmou.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 登录返回DTO
 * 人员A写的示例，给其他人参考格式
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "登录返回信息")
public class LoginResponse {

    @Schema(description = "JWT Token", example = "eyJhbGciOiJIUzI1...")
    private String token;

    @Schema(description = "用户ID", example = "1")
    private Long userId;

    @Schema(description = "用户名", example = "诸葛灵眸")
    private String username;

    @Schema(description = "客户等级", example = "工银贵宾客户")
    private String customerLevel;

    @Schema(description = "信誉分", example = "100")
    private Integer creditScore;
}
