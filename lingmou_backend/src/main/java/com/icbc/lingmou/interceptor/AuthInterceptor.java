package com.icbc.lingmou.interceptor;

import com.icbc.lingmou.common.BusinessException;
import com.icbc.lingmou.common.ResultCode;
import com.icbc.lingmou.config.JwtConfig;
import com.icbc.lingmou.utils.JwtUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 登录拦截器
 * 校验请求头中的JWT Token，提取用户ID放入request属性
 *
 * 不需要登录的接口在WebConfig中排除
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuthInterceptor implements HandlerInterceptor {

    private final JwtUtils jwtUtils;
    private final JwtConfig jwtConfig;
    private final StringRedisTemplate redisTemplate;

    private static final String TOKEN_BLACKLIST_PREFIX = "auth:logout:";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // OPTIONS预检请求直接放行
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String authHeader = request.getHeader(jwtConfig.getHeader());
        if (authHeader == null || !authHeader.startsWith(jwtConfig.getTokenPrefix())) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }

        String token = authHeader.substring(jwtConfig.getTokenPrefix().length()).trim();
        if (!jwtUtils.isTokenValid(token)) {
            throw new BusinessException(ResultCode.TOKEN_INVALID);
        }

        // 检查 Token 是否已登出（Redis 黑名单）
        if (Boolean.TRUE.equals(redisTemplate.hasKey(TOKEN_BLACKLIST_PREFIX + token))) {
            log.warn("[Auth] Token 已登出, userId={}", jwtUtils.getUserIdFromToken(token));
            throw new BusinessException(ResultCode.UNAUTHORIZED, "Token 已登出，请重新登录");
        }

        // 提取用户信息放入request，后续Controller可用 request.getAttribute("userId") 获取
        Long userId = jwtUtils.getUserIdFromToken(token);
        String username = jwtUtils.getUsernameFromToken(token);
        request.setAttribute("userId", userId);
        request.setAttribute("username", username);

        return true;
    }
}

