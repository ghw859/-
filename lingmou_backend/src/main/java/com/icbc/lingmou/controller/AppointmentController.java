package com.icbc.lingmou.controller;

import com.icbc.lingmou.common.PageResult;
import com.icbc.lingmou.common.Result;
import com.icbc.lingmou.dto.request.AppointmentHistoryRequest;
import com.icbc.lingmou.dto.request.AppointmentRequest;
import com.icbc.lingmou.dto.response.AppointmentResponse;
import com.icbc.lingmou.service.AppointmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 预约控制器
 */
@Tag(name = "预约管理", description = "智能预约排队")
@RestController
@RequestMapping("/api/appointments")
@RequiredArgsConstructor
public class AppointmentController {

    private final AppointmentService appointmentService;

    @Operation(summary = "创建预约", description = "创建新的预约")
    @PostMapping
    public Result<AppointmentResponse> createAppointment(
            @Valid @RequestBody AppointmentRequest request,
            HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        AppointmentResponse response = appointmentService.createAppointment(userId, request);
        return Result.success("预约创建成功", response);
    }

    @Operation(summary = "我的预约", description = "查询当前用户的预约列表；simple=true 返回简化版状态概览")
    @GetMapping("/my")
    public Result<List<AppointmentResponse>> getMyAppointments(
            @Parameter(description = "简化模式")
            @RequestParam(required = false) Boolean simple,
            HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        List<AppointmentResponse> appointments = appointmentService.getUserAppointments(userId);
        if (Boolean.TRUE.equals(simple)) {
            appointments = appointments.stream().map(this::toSimple).toList();
        }
        return Result.success(appointments);
    }

    @Operation(summary = "预约详情", description = "查询单个预约详情")
    @GetMapping("/{id}")
    public Result<AppointmentResponse> getAppointmentById(
            @PathVariable Long id,
            HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        AppointmentResponse response = appointmentService.getAppointmentById(id, userId);
        return Result.success(response);
    }

    @Operation(summary = "取消预约", description = "取消已创建的预约")
    @DeleteMapping("/{id}")
    public Result<Void> cancelAppointment(
            @PathVariable Long id,
            HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        appointmentService.cancelAppointment(id, userId);
        return Result.success("预约已取消", null);
    }

    @Operation(summary = "查询进度", description = "查询预约办理进度")
    @GetMapping("/{id}/progress")
    public Result<AppointmentResponse> getProgress(
            @PathVariable Long id,
            HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        AppointmentResponse response = appointmentService.getProgress(id, userId);
        return Result.success(response);
    }

    @Operation(summary = "推进进度", description = "手动按钮推进办理进度（仅能推进到下一步）")
    @PutMapping("/{id}/progress")
    public Result<AppointmentResponse> advanceProgress(
            @PathVariable Long id,
            HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        AppointmentResponse response = appointmentService.advanceProgress(id, userId);
        return Result.success("进度已推进", response);
    }

    @Operation(summary = "历史预约查询", description = "分页+多条件查询历史预约；simple=true 返回简化版状态概览")
    @GetMapping("/history")
    public Result<PageResult<AppointmentResponse>> getHistory(
            @ModelAttribute AppointmentHistoryRequest request,
            HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        PageResult<AppointmentResponse> pageResult = appointmentService.getHistory(userId, request);
        if (Boolean.TRUE.equals(request.getSimple())) {
            List<AppointmentResponse> simpleRecords = pageResult.getRecords().stream()
                    .map(this::toSimple).toList();
            pageResult.setRecords(simpleRecords);
        }
        return Result.success(pageResult);
    }

    /**
     * 预约简化版：只保留状态概览必需的字段
     */
    private AppointmentResponse toSimple(AppointmentResponse r) {
        return AppointmentResponse.builder()
                .id(r.getId())
                .status(r.getStatus())
                .progressStep(r.getProgressStep())
                .businessType(r.getBusinessType())
                .appointmentDate(r.getAppointmentDate())
                .timeSlot(r.getTimeSlot())
                .branchName(r.getBranchName())
                .build();
    }
}
