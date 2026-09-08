package com.icbc.lingmou.controller;

import com.icbc.lingmou.common.Result;
import com.icbc.lingmou.dto.request.PreFormRequest;
import com.icbc.lingmou.dto.response.PreFormResponse;
import com.icbc.lingmou.service.PreFormService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 预填单控制器
 */
@Tag(name = "预填单管理", description = "预填单存储")
@RestController
@RequestMapping("/api/preforms")
@RequiredArgsConstructor
public class PreFormController {

    private final PreFormService preFormService;

    @Operation(summary = "创建预填单", description = "创建新的预填单")
    @PostMapping
    public Result<PreFormResponse> createPreForm(
            @Valid @RequestBody PreFormRequest request,
            HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        PreFormResponse response = preFormService.createPreForm(userId, request);
        return Result.success("预填单创建成功", response);
    }

    @Operation(summary = "我的预填单", description = "查询当前用户的预填单列表")
    @GetMapping("/my")
    public Result<List<PreFormResponse>> getMyPreForms(HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        List<PreFormResponse> preForms = preFormService.getUserPreForms(userId);
        return Result.success(preForms);
    }

    @Operation(summary = "预填单详情", description = "查询单个预填单详情")
    @GetMapping("/{id}")
    public Result<PreFormResponse> getPreFormById(
            @PathVariable Long id,
            HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        PreFormResponse response = preFormService.getPreFormById(id, userId);
        return Result.success(response);
    }

    @Operation(summary = "更新预填单", description = "更新已有的预填单")
    @PutMapping("/{id}")
    public Result<PreFormResponse> updatePreForm(
            @PathVariable Long id,
            @Valid @RequestBody PreFormRequest request,
            HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        PreFormResponse response = preFormService.updatePreForm(id, userId, request);
        return Result.success("预填单更新成功", response);
    }

    @Operation(summary = "删除预填单", description = "删除预填单")
    @DeleteMapping("/{id}")
    public Result<Void> deletePreForm(
            @PathVariable Long id,
            HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        preFormService.deletePreForm(id, userId);
        return Result.success("预填单已删除", null);
    }
}
