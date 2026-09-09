package com.icbc.lingmou.controller;

import com.icbc.lingmou.common.BusinessException;
import com.icbc.lingmou.common.Result;
import com.icbc.lingmou.common.ResultCode;
import com.icbc.lingmou.dto.response.SimpleBranchResponse;
import com.icbc.lingmou.entity.Branch;
import com.icbc.lingmou.service.BranchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 网点控制器
 */
@Tag(name = "网点管理", description = "查询网点信息")
@RestController
@RequestMapping("/api/branches")
@RequiredArgsConstructor
public class BranchController {

    private final BranchService branchService;

    @Operation(summary = "查询所有网点", description = "获取全部4个网点列表；simple=true 返回简化版状态概览")
    @GetMapping
    public Result<?> getAllBranches(
            @Parameter(description = "简化模式：只返回 id+branchCode+name+busyLevel+currentQueue")
            @RequestParam(required = false) Boolean simple) {
        if (Boolean.TRUE.equals(simple)) {
            List<SimpleBranchResponse> list = branchService.findAllSimple();
            return Result.success(list);
        }
        List<Branch> branches = branchService.findAll();
        return Result.success(branches);
    }

    @Operation(summary = "查询单个网点", description = "根据ID获取网点详情")
    @GetMapping("/{id}")
    public Result<Branch> getBranchById(@PathVariable Long id) {
        Branch branch = branchService.findById(id);
        if (branch == null) {
            throw new BusinessException(ResultCode.BRANCH_NOT_FOUND);
        }
        return Result.success(branch);
    }

    @Operation(summary = "推荐网点", description = "按繁忙程度排序，推荐空闲的网点；simple=true 返回简化版")
    @GetMapping("/recommend")
    public Result<?> getRecommendBranches(
            @Parameter(description = "简化模式")
            @RequestParam(required = false) Boolean simple) {
        if (Boolean.TRUE.equals(simple)) {
            return Result.success(branchService.findAllOrderByBusyLevelSimple());
        }
        List<Branch> branches = branchService.findAllOrderByBusyLevel();
        return Result.success(branches);
    }
}
