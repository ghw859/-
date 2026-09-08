package com.icbc.lingmou.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.icbc.lingmou.common.BusinessException;
import com.icbc.lingmou.common.ResultCode;
import com.icbc.lingmou.dto.request.AppointmentHistoryRequest;
import com.icbc.lingmou.dto.request.AppointmentRequest;
import com.icbc.lingmou.dto.response.AppointmentResponse;
import com.icbc.lingmou.dto.response.PageResponse;
import com.icbc.lingmou.entity.Appointment;
import com.icbc.lingmou.entity.Branch;
import com.icbc.lingmou.mapper.AppointmentMapper;
import com.icbc.lingmou.mapper.BranchMapper;
import com.icbc.lingmou.service.AppointmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 预约Service实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AppointmentServiceImpl implements AppointmentService {

    private final AppointmentMapper appointmentMapper;
    private final BranchMapper branchMapper;

    private static final int MAX_PER_SLOT = 10; // 每时段最多10人

    @Override
    @Transactional
    public AppointmentResponse createAppointment(Long userId, AppointmentRequest request) {
        // 1. 检查网点是否存在
        Branch branch = branchMapper.selectById(request.getBranchId());
        if (branch == null) {
            throw new BusinessException(ResultCode.BRANCH_NOT_FOUND);
        }

        // 2. 检查同一用户同日同时段是否已有预约（跨网点）
        if (hasConflictAppointment(userId,
            request.getAppointmentDate().toString(),
            request.getTimeSlot())) {
            throw new BusinessException(ResultCode.APPOINTMENT_CONFLICT);
        }

        // 3. 检查时段是否已满
        if (isSlotFull(request.getBranchId(),
            request.getAppointmentDate().toString(),
            request.getTimeSlot())) {
            throw new BusinessException(ResultCode.SLOT_FULL);
        }

        // 4. 创建预约
        Appointment appointment = new Appointment();
        appointment.setUserId(userId);
        appointment.setBranchId(request.getBranchId());
        appointment.setBusinessType(request.getBusinessType());
        appointment.setAppointmentDate(request.getAppointmentDate());
        appointment.setTimeSlot(request.getTimeSlot());
        appointment.setStatus("VIRTUAL");
        appointment.setProgressStep(0);

        // 生成排队号
        String queueNumber = generateQueueNumber(request.getBranchId());
        appointment.setQueueNumber(queueNumber);

        // 生成凭证号
        String voucherNum = generateVoucherNum();
        appointment.setVoucherNum(voucherNum);

        appointmentMapper.insert(appointment);

        return toResponse(appointment, branch.getName());
    }

    @Override
    public List<AppointmentResponse> getUserAppointments(Long userId) {
        List<Appointment> appointments = appointmentMapper.selectList(
            new LambdaQueryWrapper<Appointment>()
                .eq(Appointment::getUserId, userId)
                .orderByDesc(Appointment::getCreatedAt)
        );

        return appointments.stream()
            .map(apt -> {
                Branch branch = branchMapper.selectById(apt.getBranchId());
                String branchName = branch != null ? branch.getName() : "";
                return toResponse(apt, branchName);
            })
            .collect(Collectors.toList());
    }

    @Override
    public AppointmentResponse getAppointmentById(Long id, Long userId) {
        Appointment appointment = appointmentMapper.selectById(id);
        if (appointment == null) {
            throw new BusinessException(ResultCode.APPOINTMENT_NOT_FOUND);
        }

        // 强制校验：只能查看自己的预约
        if (!appointment.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.FORBIDDEN);
        }

        Branch branch = branchMapper.selectById(appointment.getBranchId());
        return toResponse(appointment, branch != null ? branch.getName() : "");
    }

    @Override
    @Transactional
    public void cancelAppointment(Long id, Long userId) {
        Appointment appointment = appointmentMapper.selectById(id);
        if (appointment == null) {
            throw new BusinessException(ResultCode.APPOINTMENT_NOT_FOUND);
        }

        // 强制校验：只能取消自己的预约
        if (!appointment.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.FORBIDDEN);
        }

        // 检查状态：已取消、已完成、已过期的不能取消
        String status = appointment.getStatus();
        if ("CANCELED".equals(status) || "COMPLETED".equals(status) || "EXPIRED".equals(status)) {
            throw new BusinessException(ResultCode.APPOINTMENT_CANCELLED);
        }

        appointment.setStatus("CANCELED");
        appointmentMapper.updateById(appointment);
    }

    @Override
    public String generateQueueNumber(Long branchId) {
        // 排队号格式：网点编号 + 当日序号（如 A001）
        String prefix = String.valueOf(branchId); // 简单处理

        // 查询该网点今日预约数量
        LocalDate today = LocalDate.now();
        Long count = appointmentMapper.selectCount(
            new LambdaQueryWrapper<Appointment>()
                .eq(Appointment::getBranchId, branchId)
                .eq(Appointment::getAppointmentDate, today)
        );

        return prefix + String.format("%03d", count + 1);
    }

    @Override
    public boolean isSlotFull(Long branchId, String date, String timeSlot) {
        Long count = appointmentMapper.selectCount(
            new LambdaQueryWrapper<Appointment>()
                .eq(Appointment::getBranchId, branchId)
                .eq(Appointment::getAppointmentDate, LocalDate.parse(date))
                .eq(Appointment::getTimeSlot, timeSlot)
                .notIn(Appointment::getStatus, "CANCELED", "EXPIRED")
        );
        return count >= MAX_PER_SLOT;
    }

    @Override
    public boolean hasConflictAppointment(Long userId, String date, String timeSlot) {
        Long count = appointmentMapper.selectCount(
            new LambdaQueryWrapper<Appointment>()
                .eq(Appointment::getUserId, userId)
                .eq(Appointment::getAppointmentDate, LocalDate.parse(date))
                .eq(Appointment::getTimeSlot, timeSlot)
                .notIn(Appointment::getStatus, "CANCELED", "EXPIRED")
        );
        return count > 0;
    }

    @Override
    public AppointmentResponse getProgress(Long id, Long userId) {
        Appointment appointment = appointmentMapper.selectById(id);
        if (appointment == null) {
            throw new BusinessException(ResultCode.APPOINTMENT_NOT_FOUND);
        }
        if (!appointment.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.FORBIDDEN);
        }
        Branch branch = branchMapper.selectById(appointment.getBranchId());
        return toResponse(appointment, branch != null ? branch.getName() : "");
    }

    @Override
    @Transactional
    public AppointmentResponse advanceProgress(Long id, Long userId) {
        Appointment appointment = appointmentMapper.selectById(id);
        if (appointment == null) {
            throw new BusinessException(ResultCode.APPOINTMENT_NOT_FOUND);
        }
        if (!appointment.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.FORBIDDEN);
        }

        Integer currentStep = appointment.getProgressStep();
        if (currentStep >= 4) {
            throw new BusinessException(ResultCode.APPOINTMENT_COMPLETED);
        }

        // 手动推进一步
        appointment.setProgressStep(currentStep + 1);

        // 根据步骤更新状态
        switch (appointment.getProgressStep()) {
            case 1 -> appointment.setStatus("ACTIVE");
            case 2 -> appointment.setStatus("CALLED");
            case 3 -> appointment.setStatus("PROCESSING");
            case 4 -> appointment.setStatus("COMPLETED");
        }

        appointmentMapper.updateById(appointment);

        Branch branch = branchMapper.selectById(appointment.getBranchId());
        return toResponse(appointment, branch != null ? branch.getName() : "");
    }

    /**
     * 生成凭证号
     */
    private String generateVoucherNum() {
        return "V" + System.currentTimeMillis();
    }

    @Override
    public PageResponse<AppointmentResponse> getHistory(Long userId, AppointmentHistoryRequest request) {
        int pageNum = request.getPageNum() != null ? request.getPageNum() : 1;
        int pageSize = request.getPageSize() != null ? request.getPageSize() : 10;

        LambdaQueryWrapper<Appointment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Appointment::getUserId, userId); // 强制按userId过滤

        if (request.getBranchId() != null) {
            wrapper.eq(Appointment::getBranchId, request.getBranchId());
        }
        if (StringUtils.hasText(request.getBusinessType())) {
            wrapper.eq(Appointment::getBusinessType, request.getBusinessType());
        }
        if (StringUtils.hasText(request.getStatus())) {
            wrapper.eq(Appointment::getStatus, request.getStatus());
        }
        if (request.getStartDate() != null) {
            wrapper.ge(Appointment::getAppointmentDate, request.getStartDate());
        }
        if (request.getEndDate() != null) {
            wrapper.le(Appointment::getAppointmentDate, request.getEndDate());
        }

        wrapper.orderByDesc(Appointment::getCreatedAt);

        IPage<Appointment> page = new Page<>(pageNum, pageSize);
        IPage<Appointment> result = appointmentMapper.selectPage(page, wrapper);

        long total = result.getTotal();
        int totalPages = (int) Math.ceil((double) total / pageSize);

        return PageResponse.<AppointmentResponse>builder()
            .pageNum(pageNum)
            .pageSize(pageSize)
            .total(total)
            .totalPages(totalPages)
            .records(result.getRecords().stream().map(apt -> {
                Branch branch = branchMapper.selectById(apt.getBranchId());
                String branchName = branch != null ? branch.getName() : "";
                return toResponse(apt, branchName);
            }).toList())
            .build();
    }

    /**
     * 转换为响应DTO
     */
    private AppointmentResponse toResponse(Appointment appointment, String branchName) {
        return AppointmentResponse.builder()
            .id(appointment.getId())
            .userId(appointment.getUserId())
            .branchId(appointment.getBranchId())
            .branchName(branchName)
            .businessType(appointment.getBusinessType())
            .appointmentDate(appointment.getAppointmentDate())
            .timeSlot(appointment.getTimeSlot())
            .queueNumber(appointment.getQueueNumber())
            .status(appointment.getStatus())
            .voucherNum(appointment.getVoucherNum())
            .progressStep(appointment.getProgressStep())
            .createdAt(appointment.getCreatedAt())
            .build();
    }
}
