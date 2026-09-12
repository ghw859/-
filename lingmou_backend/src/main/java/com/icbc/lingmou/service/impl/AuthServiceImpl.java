package com.icbc.lingmou.service.impl;

import com.icbc.lingmou.common.BusinessException;
import com.icbc.lingmou.common.ResultCode;
import com.icbc.lingmou.config.JwtConfig;
import com.icbc.lingmou.dto.request.LoginRequest;
import com.icbc.lingmou.dto.request.RegisterRequest;
import com.icbc.lingmou.dto.response.AuthResponse;
import com.icbc.lingmou.dto.response.UserInfoResponse;
import com.icbc.lingmou.entity.User;
import com.icbc.lingmou.service.AuthService;
import com.icbc.lingmou.service.UserService;
import com.icbc.lingmou.utils.JwtUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * 认证Service实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;
    private final JwtConfig jwtConfig;
    private final StringRedisTemplate redisTemplate;

    private static final String TOKEN_BLACKLIST_PREFIX = "auth:logout:";

    @Override
    public AuthResponse login(LoginRequest request) {
        // 查询用户
        User user = userService.findByUsername(request.getUsername());
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_EXIST);
        }

        // 校验密码
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BusinessException(ResultCode.PASSWORD_ERROR);
        }

        // 生成Token
        String token = jwtUtils.generateToken(user.getId(), user.getUsername(), user.getRole());

        return buildAuthResponse(user, token);
    }

    @Override
    public AuthResponse register(RegisterRequest request) {
        // 检查用户名是否存在
        if (userService.existsByUsername(request.getUsername())) {
            throw new BusinessException(ResultCode.USER_EXIST);
        }

        // 检查手机号是否存在
        if (userService.existsByPhone(request.getPhone())) {
            throw new BusinessException(ResultCode.PHONE_EXIST);
        }

        // 创建用户
        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(request.getPassword()); // 加密在register方法里处理
        user.setRealName(request.getRealName());
        user.setPhone(request.getPhone());
        user.setIdCard(request.getIdCard());

        user = userService.register(user);

        // 生成Token
        String token = jwtUtils.generateToken(user.getId(), user.getUsername(), user.getRole());

        return buildAuthResponse(user, token);
    }

    @Override
    public void logout(String token) {
        // 将token加入黑名单
        long expiration = jwtConfig.getExpiration();
        try {
            redisTemplate.opsForValue().set(
                TOKEN_BLACKLIST_PREFIX + token,
                "1",
                expiration,
                TimeUnit.MILLISECONDS
            );
        } catch (Exception e) {
            // Redis 不可用时不阻断登出：前端会清掉本地登录态，该 Token 到期前仍有效，这里只告警
            log.warn("[Auth] Redis 不可用，登出黑名单写入失败，Token 将在自然过期前保持有效: {}", e.getMessage());
        }
    }

    /**
     * 检查Token是否在黑名单
     */
    public boolean isTokenBlacklisted(String token) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(TOKEN_BLACKLIST_PREFIX + token));
    }

    private AuthResponse buildAuthResponse(User user, String token) {
        UserInfoResponse userInfo = UserInfoResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .realName(user.getRealName())
                .phone(user.getPhone())
                .role(user.getRole())
                .customerLevel(user.getCustomerLevel())
                .creditScore(user.getCreditScore())
                .elderlyMode(user.getElderlyMode())
                .build();

        return AuthResponse.builder()
                .token(token)
                .expiresIn(jwtConfig.getExpiration() / 1000)
                .userInfo(userInfo)
                .build();
    }
}
