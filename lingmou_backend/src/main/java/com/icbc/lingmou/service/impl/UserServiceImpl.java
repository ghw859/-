package com.icbc.lingmou.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.icbc.lingmou.entity.User;
import com.icbc.lingmou.mapper.UserMapper;
import com.icbc.lingmou.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * 用户Service实现
 */
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    /** 新用户初始信用分（与 users.credit_score 列 DEFAULT 保持一致） */
    private static final int DEFAULT_CREDIT_SCORE = 90;

    @Override
    public User findByUsername(String username) {
        return userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getUsername, username));
    }

    @Override
    public User findByPhone(String phone) {
        return userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getPhone, phone));
    }

    @Override
    public User findById(Long id) {
        return userMapper.selectById(id);
    }

    @Override
    public User register(User user) {
        // 密码加密
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        // 设置默认角色
        if (user.getRole() == null) {
            user.setRole("CUSTOMER");
        }
        // 设置默认客户级别
        if (user.getCustomerLevel() == null) {
            user.setCustomerLevel("NORMAL");
        }
        // 设置默认信用分：初始90，办结+3等正向行为有成长空间（上限100）
        if (user.getCreditScore() == null) {
            user.setCreditScore(DEFAULT_CREDIT_SCORE);
        }
        // 设置默认老年模式
        if (user.getElderlyMode() == null) {
            user.setElderlyMode(0);
        }
        userMapper.insert(user);
        return user;
    }

    @Override
    public boolean existsByUsername(String username) {
        return userMapper.selectCount(
                new LambdaQueryWrapper<User>().eq(User::getUsername, username)) > 0;
    }

    @Override
    public boolean existsByPhone(String phone) {
        return userMapper.selectCount(
                new LambdaQueryWrapper<User>().eq(User::getPhone, phone)) > 0;
    }
}
