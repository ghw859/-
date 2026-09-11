package com.icbc.lingmou.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.icbc.lingmou.dto.response.BranchResponse;
import com.icbc.lingmou.dto.response.SimpleBranchResponse;
import com.icbc.lingmou.entity.Branch;
import com.icbc.lingmou.mapper.BranchMapper;
import com.icbc.lingmou.service.BranchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

/**
 * 网点Service实现
 * Branch entity → BranchResponse 映射 + JSON 数组解析 + Java 层排序
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BranchServiceImpl implements BranchService {

    private final BranchMapper branchMapper;
    private final ObjectMapper objectMapper;

    @Override
    public List<BranchResponse> findAll() {
        List<Branch> branches = branchMapper.selectList(
            new LambdaQueryWrapper<Branch>().orderByAsc(Branch::getId)
        );
        return branches.stream().map(this::toResponse).toList();
    }

    @Override
    public List<SimpleBranchResponse> findAllSimple() {
        return findAll().stream().map(this::toSimple).toList();
    }

    @Override
    public BranchResponse findById(String id) {
        Branch branch = findBranchByAnyId(id);
        return branch != null ? toResponse(branch) : null;
    }

    @Override
    public List<BranchResponse> findAllOrderByBusyLevel() {
        List<Branch> branches = branchMapper.selectList(
            new LambdaQueryWrapper<Branch>().orderByAsc(Branch::getId)
        );
        // Java 层排序：free(0) → moderate(1) → busy(2)
        branches.sort(Comparator.comparingInt(this::statusOrder));
        return branches.stream().map(this::toResponse).toList();
    }

    @Override
    public List<SimpleBranchResponse> findAllOrderByBusyLevelSimple() {
        return findAllOrderByBusyLevel().stream().map(this::toSimple).toList();
    }

    @Override
    public Branch findByDbId(Long dbId) {
        return branchMapper.selectById(dbId);
    }

    // =================== 映射方法 ===================

    private BranchResponse toResponse(Branch b) {
        return BranchResponse.builder()
                .id(b.getBranchCode())             // branch_code "b1" → id
                .name(b.getName())
                .status(b.getBusyLevel())           // free/moderate/busy 直传
                .distance(b.getDistance())
                .services(parseStringList(b.getServices()))
                .wait(b.getWaitTime())
                .flow(b.getFlowCount())
                .reserve(b.getReserveCount())
                .window(b.getWindowInfo())
                .trend(parseIntList(b.getTrend()))
                .address(b.getAddress())
                .phone(b.getPhone())
                .hours(b.getHours())
                .icon(b.getIcon())
                .iconBg(b.getIconBg())
                .iconColor(b.getIconColor())
                .favorite(b.getFavorite() != null && b.getFavorite() == 1)
                .build();
    }

    private SimpleBranchResponse toSimple(BranchResponse r) {
        return SimpleBranchResponse.builder()
                .id(r.getId())
                .name(r.getName())
                .status(r.getStatus())
                .distance(r.getDistance())
                .wait(r.getWait())
                .flow(r.getFlow())
                .window(r.getWindow())
                .build();
    }

    // =================== 工具方法 ===================

    /**
     * free=0, moderate=1, busy=2，其他=3
     */
    private int statusOrder(Branch b) {
        String s = b.getBusyLevel();
        if (s == null) return 3;
        return switch (s.toLowerCase()) {
            case "free" -> 0;
            case "moderate" -> 1;
            case "busy" -> 2;
            default -> 3;
        };
    }

    /**
     * 支持 "b1" 字符串或 "1" 数字两种形式查找
     */
    private Branch findBranchByAnyId(String id) {
        if (id == null || id.isBlank()) return null;
        // 先按 branch_code 查
        Branch b = branchMapper.selectOne(
            new LambdaQueryWrapper<Branch>().eq(Branch::getBranchCode, id)
        );
        if (b != null) return b;
        // 再按数字主键查
        try {
            Long dbId = Long.parseLong(id);
            return branchMapper.selectById(dbId);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private List<String> parseStringList(String json) {
        if (json == null || json.isBlank()) return List.of();
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            log.warn("解析 services JSON 失败: {}", json, e);
            return List.of();
        }
    }

    private List<Integer> parseIntList(String json) {
        if (json == null || json.isBlank()) return List.of();
        try {
            return objectMapper.readValue(json, new TypeReference<List<Integer>>() {});
        } catch (Exception e) {
            log.warn("解析 trend JSON 失败: {}", json, e);
            return List.of();
        }
    }
}
