package com.icbc.lingmou.service;

import com.icbc.lingmou.dto.request.LoginRequest;
import com.icbc.lingmou.dto.request.RegisterRequest;
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
     * 用户注册
     */
    AuthResponse register(RegisterRequest request);

    /**
     * 用户登出
     */
    void logout(String token);
}
