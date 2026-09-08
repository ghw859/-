package com.icbc.lingmou.service;

import com.icbc.lingmou.entity.User;

/**
 * 用户Service接口
 */
public interface UserService {

    /**
     * 根据用户名查询用户
     */
    User findByUsername(String username);

    /**
     * 根据手机号查询用户
     */
    User findByPhone(String phone);

    /**
     * 根据ID查询用户
     */
    User findById(Long id);

    /**
     * 注册用户
     */
    User register(User user);

    /**
     * 判断用户名是否存在
     */
    boolean existsByUsername(String username);

    /**
     * 判断手机号是否存在
     */
    boolean existsByPhone(String phone);
}
