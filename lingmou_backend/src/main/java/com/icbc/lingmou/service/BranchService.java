package com.icbc.lingmou.service;

import com.icbc.lingmou.dto.response.BranchResponse;
import com.icbc.lingmou.dto.response.SimpleBranchResponse;
import com.icbc.lingmou.entity.Branch;

import java.util.List;

/**
 * 网点Service接口
 */
public interface BranchService {

    /**
     * 查询所有网点（返回前端契约 DTO）
     */
    List<BranchResponse> findAll();

    /**
     * 查询所有网点（简化版）
     */
    List<SimpleBranchResponse> findAllSimple();

    /**
     * 根据ID查询网点（支持 "b1" 字符串或数字 "1"）
     */
    BranchResponse findById(String id);

    /**
     * 按繁忙程度排序（free → moderate → busy）
     */
    List<BranchResponse> findAllOrderByBusyLevel();

    /**
     * 按繁忙程度排序（简化版）
     */
    List<SimpleBranchResponse> findAllOrderByBusyLevelSimple();

    /**
     * 根据数字主键查询网点（内部调用，如预约关联）
     */
    Branch findByDbId(Long dbId);
}
