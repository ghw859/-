package com.icbc.lingmou.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 微信登录请求DTO
 * 小程序端通过 wx.login 获取 code，并将用户确认后的昵称/头像一并提交
 */
@Data
@Schema(description = "微信登录请求参数")
public class WxLoginRequest {

    @Schema(description = "微信昵称", example = "微信用户")
    private String nickName;

    @Schema(description = "微信头像URL")
    private String avatarUrl;

    @Schema(description = "wx.login 返回的临时登录凭证 code")
    private String code;

    /**
     * 设备稳定标识：微信 code 每次登录都会变化，不能作为用户唯一标识。
     * 在小程序端首次登录时生成并持久化，用于在未配置 AppSecret 的开发/演示环境下
     * 保证同一设备多次登录映射到同一个账号。
     */
    @Schema(description = "客户端设备稳定标识（开发环境兜底用）")
    private String deviceId;
}
