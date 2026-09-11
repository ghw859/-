package com.icbc.lingmou.controller;

import com.icbc.lingmou.common.BusinessException;
import com.icbc.lingmou.common.Result;
import com.icbc.lingmou.common.ResultCode;
import com.icbc.lingmou.dto.response.BranchResponse;
import com.icbc.lingmou.dto.response.SimpleBranchResponse;
import com.icbc.lingmou.service.BranchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 网点控制器
 * 响应格式对齐前端 branch.ts interface Branch
 */
@Tag(name = "网点管理", description = "查询网点信息")
@RestController
@RequestMapping("/api/branches")
@RequiredArgsConstructor
public class BranchController {

    private final BranchService branchService;

    @Operation(summary = "查询所有网点", description = "返回6个北京网点，字段对齐前端 Branch interface；simple=true 返回简化版")
    @GetMapping
    public Result<?> getAllBranches(
            @Parameter(description = "简化模式：只返回 id+name+status+distance+wait+flow+window")
            @RequestParam(required = false) Boolean simple) {
        if (Boolean.TRUE.equals(simple)) {
            List<SimpleBranchResponse> list = branchService.findAllSimple();
            return Result.success(list);
        }
        List<BranchResponse> branches = branchService.findAll();
        return Result.success(branches);
    }

    @Operation(summary = "查询单个网点", description = "支持 b1 字符串或 1 数字ID")
    @GetMapping("/{id}")
    public Result<BranchResponse> getBranchById(@PathVariable String id) {
        BranchResponse branch = branchService.findById(id);
        if (branch == null) {
            throw new BusinessException(ResultCode.BRANCH_NOT_FOUND);
        }
        return Result.success(branch);
    }

    @Operation(summary = "推荐网点", description = "按繁忙程度排序 free→moderate→busy；simple=true 返回简化版")
    @GetMapping("/recommend")
    public Result<?> getRecommendBranches(
            @Parameter(description = "简化模式")
            @RequestParam(required = false) Boolean simple) {
        if (Boolean.TRUE.equals(simple)) {
            return Result.success(branchService.findAllOrderByBusyLevelSimple());
        }
        List<BranchResponse> branches = branchService.findAllOrderByBusyLevel();
        return Result.success(branches);
    }
}
