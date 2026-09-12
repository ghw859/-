package com.icbc.lingmou.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.icbc.lingmou.entity.Appointment;
import com.icbc.lingmou.mapper.AppointmentMapper;
import com.icbc.lingmou.service.CreditService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

/**
 * 预约过期定时任务
 * 每5分钟扫描一次，将过期未到的预约标记为 EXPIRED 并扣除信用分
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AppointmentExpireTask {

    private final AppointmentMapper appointmentMapper;
    private final CreditService creditService;

    /**
     * 每5分钟执行一次
     * cron: 秒 分 时 日 月 周
     */
    @Scheduled(cron = "0 */5 * * * ?")
    public void expireAppointments() {
        LocalDate today = LocalDate.now();

        // 查找已过预约日期且仍在进行中的预约
        List<Appointment> expiredList = appointmentMapper.selectList(
            new LambdaQueryWrapper<Appointment>()
                .lt(Appointment::getAppointmentDate, today)
                .in(Appointment::getStatus, "VIRTUAL", "ACTIVE")
        );

        if (expiredList.isEmpty()) {
            return;
        }

        log.info("扫描到 {} 条过期预约，开始处理", expiredList.size());

        for (Appointment apt : expiredList) {
            // 条件更新（CAS）：仅当记录仍处于"待办理"状态时才置为过期。
            // 直接 updateById 是"先读后写"，多实例或重复扫描时同一条预约会被扣两次分
            // （实测两个实例同时扫描 → 同一预约产生两条 -10 记录）。
            int updated = appointmentMapper.update(null,
                new LambdaUpdateWrapper<Appointment>()
                    .eq(Appointment::getId, apt.getId())
                    .in(Appointment::getStatus, "VIRTUAL", "ACTIVE")
                    .set(Appointment::getStatus, "EXPIRED")
            );

            if (updated == 0) {
                // 已被其他实例/上一轮处理过，跳过扣分，保证幂等
                continue;
            }

            // 信用分规则：超时未到扣10分
            creditService.autoAdjust(apt.getUserId(), -10, "预约超时未到");

            log.info("预约已过期: id={}, userId={}, date={}",
                apt.getId(), apt.getUserId(), apt.getAppointmentDate());
        }
    }
}
