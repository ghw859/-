package com.icbc.lingmou.service;

import com.icbc.lingmou.dto.response.SimpleBranchResponse;
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
     * 查询所有网点（简化版：只返回状态概览）
     */
    List<SimpleBranchResponse> findAllSimple();

    /**
     * 根据ID查询网点
     */
    Branch findById(Long id);

    /**
     * 按繁忙程度排序
     */
    List<Branch> findAllOrderByBusyLevel();

    /**
     * 按繁忙程度排序（简化版：只返回状态概览）
     */
    List<SimpleBranchResponse> findAllOrderByBusyLevelSimple();
}
