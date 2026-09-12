package com.icbc.lingmou.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.icbc.lingmou.common.BusinessException;
import com.icbc.lingmou.common.PageResult;
import com.icbc.lingmou.common.ResultCode;
import com.icbc.lingmou.dto.request.AppointmentHistoryRequest;
import com.icbc.lingmou.dto.request.AppointmentRequest;
import com.icbc.lingmou.dto.response.AppointmentResponse;
import com.icbc.lingmou.entity.Appointment;
import com.icbc.lingmou.entity.Branch;
import com.icbc.lingmou.entity.User;
import com.icbc.lingmou.mapper.AppointmentMapper;
import com.icbc.lingmou.mapper.BranchMapper;
import com.icbc.lingmou.mapper.UserMapper;
import com.icbc.lingmou.service.AppointmentService;
import com.icbc.lingmou.service.CreditService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;
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
    private final UserMapper userMapper;
    private final CreditService creditService;

    private static final int MAX_PER_SLOT = 10; // 每时段最多10人
    private static final int CREDIT_LIMIT = 60; // 信用分低于60限制预约
    private static final int DEFAULT_CREDIT_SCORE = 90; // 空值兜底初始分（与注册逻辑一致）

    /**
     * 预约创建的分段锁（striped lock）
     * "查冲突 → 查余量 → 生成排队号 → 落库"这一串是典型的 check-then-act，
     * 并发下两个请求会同时通过容量校验，导致时段超卖、排队号重号。
     * 这里按 (网点, 日期) 分段加锁把它们变成串行，锁对象数量固定，不会无限增长。
     */
    private static final Object[] BOOKING_LOCKS = new Object[64];

    static {
        for (int i = 0; i < BOOKING_LOCKS.length; i++) {
            BOOKING_LOCKS[i] = new Object();
        }
    }

    private Object bookingLock(Long branchId, LocalDate date) {
        return BOOKING_LOCKS[Math.floorMod(Objects.hash(branchId, date), BOOKING_LOCKS.length)];
    }

    /**
     * 创建预约。
     *
     * 注意：这里刻意不加 @Transactional —— 整段只有一条写操作（insert），
     * 而分段锁必须在事务提交之后才释放才有意义：若由外层事务包住，
     * 锁释放时 insert 尚未提交，后一个线程读不到前一条记录，容量校验依旧会串味。
     */
    @Override
    public AppointmentResponse createAppointment(Long userId, AppointmentRequest request) {
        // 0. 检查信用分（低于阈值限制预约）
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_EXIST);
        }
        Integer creditScore = user.getCreditScore() != null ? user.getCreditScore() : DEFAULT_CREDIT_SCORE;
        if (creditScore < CREDIT_LIMIT) {
            throw new BusinessException(ResultCode.CREDIT_TOO_LOW);
        }

        // 1. 检查网点是否存在
        Branch branch = branchMapper.selectById(request.getBranchId());
        if (branch == null) {
            throw new BusinessException(ResultCode.BRANCH_NOT_FOUND);
        }

        Appointment appointment = new Appointment();

        // 2~4. 冲突校验 / 余量校验 / 排队号生成 / 落库，必须在同一把锁内串行完成
        synchronized (bookingLock(request.getBranchId(), request.getAppointmentDate())) {
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
            appointment.setUserId(userId);
            appointment.setBranchId(request.getBranchId());
            appointment.setBusinessType(request.getBusinessType());
            appointment.setAppointmentDate(request.getAppointmentDate());
            appointment.setTimeSlot(request.getTimeSlot());
            appointment.setStatus("VIRTUAL");
            appointment.setProgressStep(0);

            // 生成排队号（按 网点 + 预约日期 计数，与"今天"无关）
            String queueNumber = generateQueueNumber(request.getBranchId(), request.getAppointmentDate());
            appointment.setQueueNumber(queueNumber);

            // 生成凭证号
            String voucherNum = generateVoucherNum();
            appointment.setVoucherNum(voucherNum);

            appointmentMapper.insert(appointment);
        }

        return toResponse(appointment, branch.getName());
    }

    @Override
    public List<AppointmentResponse> getUserAppointments(Long userId) {
        List<Appointment> appointments = appointmentMapper.selectList(
                new LambdaQueryWrapper<Appointment>()
                        .eq(Appointment::getUserId, userId)
                        .orderByDesc(Appointment::getCreatedAt));

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

        // 检查状态：只有"未到店/已到店排队"可以取消；
        // 一旦叫号或已进办理流程，取消会打断柜员手上的工单，必须走线下
        String status = appointment.getStatus();
        if ("CANCELED".equals(status)) {
            throw new BusinessException(ResultCode.APPOINTMENT_CANCELLED);
        }
        if ("COMPLETED".equals(status)) {
            throw new BusinessException(ResultCode.APPOINTMENT_COMPLETED, "预约已办理完成，无法取消");
        }
        if ("EXPIRED".equals(status)) {
            throw new BusinessException(ResultCode.APPOINTMENT_EXPIRED, "预约已过期，无法取消");
        }
        if ("CALLED".equals(status) || "PROCESSING".equals(status)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "已叫号或办理中的预约无法自助取消，请联系大堂经理");
        }

        appointment.setStatus("CANCELED");
        appointmentMapper.updateById(appointment);

        // 信用分规则：主动取消扣5分
        creditService.autoAdjust(userId, -5, "预约取消");
    }

    @Override
    public String generateQueueNumber(Long branchId, LocalDate appointmentDate) {
        // 排队号格式：网点编号 + 该网点该日序号（如网点1当日第1位 → 1001）
        String prefix = String.valueOf(branchId);

        // 必须按"预约日期"计数，而不是按"今天"计数：
        // 之前用 LocalDate.now() 统计，导致预约 8 天后的号仍然从 1001 起，不同预约重号
        Long count = appointmentMapper.selectCount(
                new LambdaQueryWrapper<Appointment>()
                        .eq(Appointment::getBranchId, branchId)
                        .eq(Appointment::getAppointmentDate, appointmentDate));

        return prefix + String.format("%03d", count + 1);
    }

    @Override
    public boolean isSlotFull(Long branchId, String date, String timeSlot) {
        Long count = appointmentMapper.selectCount(
                new LambdaQueryWrapper<Appointment>()
                        .eq(Appointment::getBranchId, branchId)
                        .eq(Appointment::getAppointmentDate, LocalDate.parse(date))
                        .eq(Appointment::getTimeSlot, timeSlot)
                        .notIn(Appointment::getStatus, "CANCELED", "EXPIRED"));
        return count >= MAX_PER_SLOT;
    }

    @Override
    public boolean hasConflictAppointment(Long userId, String date, String timeSlot) {
        Long count = appointmentMapper.selectCount(
                new LambdaQueryWrapper<Appointment>()
                        .eq(Appointment::getUserId, userId)
                        .eq(Appointment::getAppointmentDate, LocalDate.parse(date))
                        .eq(Appointment::getTimeSlot, timeSlot)
                        .notIn(Appointment::getStatus, "CANCELED", "EXPIRED"));
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
            case 4 -> {
                appointment.setStatus("COMPLETED");
                // 信用分规则：办理完成加3分
                creditService.autoAdjust(userId, 3, "预约办理完成");
            }
        }

        appointmentMapper.updateById(appointment);

        Branch branch = branchMapper.selectById(appointment.getBranchId());
        return toResponse(appointment, branch != null ? branch.getName() : "");
    }

    /**
     * 生成凭证号（保持 V+数字 格式，与 API_DAY3 契约示例一致）
     *
     * 只取 System.currentTimeMillis() 时，同毫秒内的两个请求会生成同一个凭证号，
     * 撞上 vouchers.voucher_num 的 UNIQUE 约束后整个预约直接失败，这里追加随机数避开。
     */
    private String generateVoucherNum() {
        return "V" + System.currentTimeMillis()
                + String.format("%04d", ThreadLocalRandom.current().nextInt(10000));
    }

    @Override
    public PageResult<AppointmentResponse> getHistory(Long userId, AppointmentHistoryRequest request) {
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

        List<AppointmentResponse> records = result.getRecords().stream().map(apt -> {
            Branch branch = branchMapper.selectById(apt.getBranchId());
            String branchName = branch != null ? branch.getName() : "";
            return toResponse(apt, branchName);
        }).toList();

        return PageResult.of(pageNum, pageSize, result.getTotal(), records);
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
