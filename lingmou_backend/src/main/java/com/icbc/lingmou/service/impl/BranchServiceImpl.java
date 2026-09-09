package com.icbc.lingmou.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.icbc.lingmou.dto.response.SimpleBranchResponse;
import com.icbc.lingmou.entity.Branch;
import com.icbc.lingmou.mapper.BranchMapper;
import com.icbc.lingmou.service.BranchService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 网点Service实现
 */
@Service
@RequiredArgsConstructor
public class BranchServiceImpl implements BranchService {

    private final BranchMapper branchMapper;

    @Override
    public List<Branch> findAll() {
        return branchMapper.selectList(
            new LambdaQueryWrapper<Branch>().orderByAsc(Branch::getId)
        );
    }

    @Override
    public List<SimpleBranchResponse> findAllSimple() {
        return findAll().stream().map(this::toSimple).toList();
    }

    @Override
    public Branch findById(Long id) {
        return branchMapper.selectById(id);
    }

    @Override
    public List<Branch> findAllOrderByBusyLevel() {
        // 按 IDLE -> MODERATE -> BUSY 排序
        return branchMapper.selectList(
            new LambdaQueryWrapper<Branch>()
                .orderByAsc(Branch::getBusyLevel)
        );
    }

    @Override
    public List<SimpleBranchResponse> findAllOrderByBusyLevelSimple() {
        return findAllOrderByBusyLevel().stream().map(this::toSimple).toList();
    }

    private SimpleBranchResponse toSimple(Branch b) {
        return SimpleBranchResponse.builder()
                .id(b.getId())
                .branchCode(b.getBranchCode())
                .name(b.getName())
                .busyLevel(b.getBusyLevel())
                .currentQueue(b.getCurrentQueue())
                .build();
    }
}
