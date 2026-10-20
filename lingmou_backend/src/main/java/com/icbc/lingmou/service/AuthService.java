package com.icbc.lingmou.service;

import com.icbc.lingmou.dto.request.LoginRequest;
import com.icbc.lingmou.dto.request.RegisterRequest;
import com.icbc.lingmou.dto.request.WxLoginRequest;
import com.icbc.lingmou.dto.response.AuthResponse;

/**
 * 认证Service接口
 */
public interface AuthService {

    /**
     * 用户登录
     */
    AuthResponse login(LoginRequest request);

    /**
     * 微信授权登录：首次登录自动创建账号，之后复用同一账号
     */
    AuthResponse wxLogin(WxLoginRequest request);

    /**
     * 用户注册
     */
    AuthResponse register(RegisterRequest request);

    /**
     * 用户登出
     */
    void logout(String token);
}
