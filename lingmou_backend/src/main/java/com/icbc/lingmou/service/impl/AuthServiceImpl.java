package com.icbc.lingmou.service.impl;

import com.icbc.lingmou.common.BusinessException;
import com.icbc.lingmou.common.ResultCode;
import com.icbc.lingmou.config.JwtConfig;
import com.icbc.lingmou.dto.request.LoginRequest;
import com.icbc.lingmou.dto.request.RegisterRequest;
import com.icbc.lingmou.dto.request.WxLoginRequest;
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

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;
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
    /** 微信登录用户名前缀 */
    private static final String WX_USER_PREFIX = "wx_";

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
    public AuthResponse wxLogin(WxLoginRequest request) {
        // 1. 计算稳定的微信用户标识（username）
        String openIdSeed = resolveIdentitySeed(request);
        String username = WX_USER_PREFIX + shortHash(openIdSeed);

        // 2. 查找或创建用户
        User user = userService.findByUsername(username);
        boolean isNewUser = false;
        if (user == null) {
            // 首次微信授权：自动创建账号（密码设为随机值，微信用户不走密码登录）
            String randomPassword = UUID.randomUUID().toString().replace("-", "");
            User newUser = new User();
            newUser.setUsername(username);
            newUser.setPassword(randomPassword);
            newUser.setRealName(request.getNickName());
            newUser.setAvatar(request.getAvatarUrl());
            user = userService.register(newUser);
            isNewUser = true;
        } else {
            // 老用户：同步最新的昵称/头像（有变化才更新，避免无意义写库）
            boolean changed = false;
            if (request.getNickName() != null && !request.getNickName().equals(user.getRealName())) {
                user.setRealName(request.getNickName());
                changed = true;
            }
            if (request.getAvatarUrl() != null && !request.getAvatarUrl().equals(user.getAvatar())) {
                user.setAvatar(request.getAvatarUrl());
                changed = true;
            }
            if (changed) {
                userService.updateProfile(user);
            }
        }

        // 3. 生成Token
        String token = jwtUtils.generateToken(user.getId(), user.getUsername(), user.getRole());

        if (isNewUser) {
            log.info("[Auth] 微信新用户注册登录: userId={}, nickName={}", user.getId(), request.getNickName());
        }
        return buildAuthResponse(user, token);
    }

    /**
     * 解析微信用户的身份种子。
     * 优先使用客户端持久化的 deviceId（同一设备多次登录保持同一账号），
     * 其次退化为微信 code（code 本身每次都变，仅兜底），最后退化为昵称。
     */
    private String resolveIdentitySeed(WxLoginRequest request) {
        if (request.getDeviceId() != null && !request.getDeviceId().trim().isEmpty()) {
            return "dev:" + request.getDeviceId().trim();
        }
        if (request.getCode() != null && !request.getCode().trim().isEmpty()) {
            return "code:" + request.getCode().trim();
        }
        return "nick:" + (request.getNickName() == null ? "unknown" : request.getNickName());
    }

    /** 将任意种子转为 16 位十六进制短哈希，保证 username 不超过 VARCHAR(50) */
    private String shortHash(String seed) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(seed.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 8; i++) {
                sb.append(String.format("%02x", digest[i]));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 必须可用，正常不会走到
            return Integer.toHexString(seed.hashCode());
        }
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
                .avatar(user.getAvatar())
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
