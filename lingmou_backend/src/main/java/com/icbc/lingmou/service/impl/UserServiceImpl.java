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

    @Override
    public User findByUsername(String username) {
        return userMapper.selectOne(
            new LambdaQueryWrapper<User>().eq(User::getUsername, username)
        );
    }

    @Override
    public User findByPhone(String phone) {
        return userMapper.selectOne(
            new LambdaQueryWrapper<User>().eq(User::getPhone, phone)
        );
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
        // 设置默认信用分
        if (user.getCreditScore() == null) {
            user.setCreditScore(100);
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
            new LambdaQueryWrapper<User>().eq(User::getUsername, username)
        ) > 0;
    }

    @Override
    public boolean existsByPhone(String phone) {
        return userMapper.selectCount(
            new LambdaQueryWrapper<User>().eq(User::getPhone, phone)
        ) > 0;
    }
}
