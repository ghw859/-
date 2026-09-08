package com.icbc.lingmou.service;

import com.icbc.lingmou.entity.Branch;

import java.util.List;

/**
 * 网点Service接口
 */
public interface BranchService {

    /**
     * 查询所有网点
     */
    List<Branch> findAll();

    /**
     * 根据ID查询网点
     */
    Branch findById(Long id);

    /**
     * 按繁忙程度排序
     */
    List<Branch> findAllOrderByBusyLevel();
}
