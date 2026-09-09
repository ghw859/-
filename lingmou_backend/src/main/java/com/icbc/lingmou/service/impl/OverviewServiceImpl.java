package com.icbc.lingmou.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.icbc.lingmou.dto.response.*;
import com.icbc.lingmou.entity.Appointment;
import com.icbc.lingmou.entity.CreditRecord;
import com.icbc.lingmou.entity.PreForm;
import com.icbc.lingmou.entity.User;
import com.icbc.lingmou.mapper.AppointmentMapper;
import com.icbc.lingmou.mapper.CreditRecordMapper;
import com.icbc.lingmou.mapper.PreFormMapper;
import com.icbc.lingmou.mapper.UserMapper;
import com.icbc.lingmou.service.OverviewService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 数据总览Service实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OverviewServiceImpl implements OverviewService {

    private final UserMapper userMapper;
    private final AppointmentMapper appointmentMapper;
    private final PreFormMapper preFormMapper;
    private final CreditRecordMapper creditRecordMapper;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("MM-dd");

    @Override
    public OverviewCardResponse getOverviewCards() {
        LocalDate today = LocalDate.now();
        LocalDate yesterday = today.minusDays(1);

        // 总用户数
        Long totalUsers = userMapper.selectCount(null);

        // 今日预约数
        Long todayAppointments = appointmentMapper.selectCount(
            new LambdaQueryWrapper<Appointment>()
                .eq(Appointment::getAppointmentDate, today)
        );

        // 总预填单数
        Long totalPreForms = preFormMapper.selectCount(null);

        // 本月办理完成数
        LocalDate monthStart = today.withDayOfMonth(1);
        Long monthlyCompleted = appointmentMapper.selectCount(
            new LambdaQueryWrapper<Appointment>()
                .ge(Appointment::getCreatedAt, monthStart.atStartOfDay())
                .eq(Appointment::getStatus, "COMPLETED")
        );

        // 昨日数据（计算变化）
        Long yesterdayUsers = userMapper.selectCount(
            new LambdaQueryWrapper<User>()
                .lt(User::getCreatedAt, today.atStartOfDay())
        );
        Long yesterdayAppointments = appointmentMapper.selectCount(
            new LambdaQueryWrapper<Appointment>()
                .eq(Appointment::getAppointmentDate, yesterday)
        );
        Long yesterdayPreForms = preFormMapper.selectCount(
            new LambdaQueryWrapper<PreForm>()
                .lt(PreForm::getCreatedAt, today.atStartOfDay())
        );
        Long yesterdayCompleted = appointmentMapper.selectCount(
            new LambdaQueryWrapper<Appointment>()
                .ge(Appointment::getCreatedAt, yesterday.withDayOfMonth(1).atStartOfDay())
                .lt(Appointment::getCreatedAt, today.atStartOfDay())
                .eq(Appointment::getStatus, "COMPLETED")
        );

        return OverviewCardResponse.builder()
            .totalUsers(totalUsers)
            .todayAppointments(todayAppointments)
            .totalPreForms(totalPreForms)
            .monthlyCompleted(monthlyCompleted)
            .usersChange((int) (totalUsers - yesterdayUsers))
            .appointmentsChange((int) (todayAppointments - yesterdayAppointments))
            .preFormsChange((int) (totalPreForms - yesterdayPreForms))
            .completedChange((int) (monthlyCompleted - yesterdayCompleted))
            .build();
    }

    @Override
    public AssetsChartResponse getAssetsChart() {
        LocalDate today = LocalDate.now();
        List<String> dates = new ArrayList<>();
        List<Integer> accounts = new ArrayList<>();
        List<Long> deposits = new ArrayList<>();
        List<Long> finances = new ArrayList<>();

        // 近7天数据
        for (int i = 6; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            dates.add(date.format(DATE_FMT));

            // 每日新增用户（开户数）
            Long newUsers = userMapper.selectCount(
                new LambdaQueryWrapper<User>()
                    .between(User::getCreatedAt, date.atStartOfDay(), date.plusDays(1).atStartOfDay())
            );
            accounts.add(newUsers.intValue());

            // 每日预约数作为业务量指标（模拟充值/理财金额）
            Long dailyAppointments = appointmentMapper.selectCount(
                new LambdaQueryWrapper<Appointment>()
                    .eq(Appointment::getAppointmentDate, date)
            );
            // 模拟数据：开户关联存款/理财
            deposits.add(dailyAppointments * 50000L); // 每人5万估算
            finances.add(dailyAppointments * 30000L); // 每人3万估算
        }

        return AssetsChartResponse.builder()
            .dates(dates)
            .accounts(accounts)
            .deposits(deposits)
            .finances(finances)
            .build();
    }

    @Override
    public CreditTrendResponse getCreditTrend() {
        LocalDate today = LocalDate.now();
        List<String> dates = new ArrayList<>();
        List<Double> avgScores = new ArrayList<>();
        List<Integer> highScoreUsers = new ArrayList<>();
        List<Integer> lowScoreUsers = new ArrayList<>();

        // 近7天趋势
        for (int i = 6; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            dates.add(date.format(DATE_FMT));

            // 当日有信用分变更的用户
            List<CreditRecord> dayRecords = creditRecordMapper.selectList(
                new LambdaQueryWrapper<CreditRecord>()
                    .between(CreditRecord::getCreatedAt, date.atStartOfDay(), date.plusDays(1).atStartOfDay())
            );

            // 简单处理：取所有用户的平均信用分（基于最新记录计算）
            List<User> allUsers = userMapper.selectList(null);
            if (!allUsers.isEmpty()) {
                double avg = allUsers.stream()
                    .mapToInt(u -> u.getCreditScore() != null ? u.getCreditScore() : 100)
                    .average()
                    .orElse(100.0);
                avgScores.add(Math.round(avg * 10) / 10.0);

                long high = allUsers.stream()
                    .filter(u -> u.getCreditScore() != null && u.getCreditScore() >= 90)
                    .count();
                highScoreUsers.add((int) high);

                long low = allUsers.stream()
                    .filter(u -> u.getCreditScore() != null && u.getCreditScore() < 70)
                    .count();
                lowScoreUsers.add((int) low);
            } else {
                avgScores.add(100.0);
                highScoreUsers.add(0);
                lowScoreUsers.add(0);
            }
        }

        return CreditTrendResponse.builder()
            .dates(dates)
            .avgScores(avgScores)
            .highScoreUsers(highScoreUsers)
            .lowScoreUsers(lowScoreUsers)
            .build();
    }

    @Override
    public BusinessPieResponse getBusinessPie() {
        // 统计各业务类型数量
        List<Appointment> allAppointments = appointmentMapper.selectList(
            new LambdaQueryWrapper<Appointment>()
        );

        Map<String, Long> businessCount = allAppointments.stream()
            .filter(a -> a.getBusinessType() != null)
            .collect(Collectors.groupingBy(Appointment::getBusinessType, Collectors.counting()));

        int total = businessCount.values().stream().mapToInt(Long::intValue).sum();

        List<BusinessPieResponse.BusinessItem> items = businessCount.entrySet().stream()
            .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
            .map(entry -> BusinessPieResponse.BusinessItem.builder()
                .businessType(entry.getKey())
                .count(entry.getValue().intValue())
                .percentage(total > 0 ? Math.round(entry.getValue() * 1000.0 / total) / 10.0 : 0.0)
                .build())
            .collect(Collectors.toList());

        return BusinessPieResponse.builder()
            .items(items)
            .build();
    }
}
