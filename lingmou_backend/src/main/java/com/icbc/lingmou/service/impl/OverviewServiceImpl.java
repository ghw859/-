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

    /** 信用分口径：与 CreditServiceImpl 保持一致 */
    private static final int DEFAULT_SCORE = 100;
    private static final int MIN_SCORE = 0;
    private static final int MAX_SCORE = 100;
    private static final int HIGH_SCORE_THRESHOLD = 90;
    private static final int LOW_SCORE_THRESHOLD = 70;

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
        LocalDate windowStart = today.minusDays(6);
        List<String> dates = new ArrayList<>();
        List<Double> avgScores = new ArrayList<>();
        List<Integer> highScoreUsers = new ArrayList<>();
        List<Integer> lowScoreUsers = new ArrayList<>();

        // 一次性取全量用户与窗口期内的信用分变更记录，避免逐日重复全表扫描
        List<User> allUsers = userMapper.selectList(null);
        List<CreditRecord> windowRecords = creditRecordMapper.selectList(
            new LambdaQueryWrapper<CreditRecord>()
                .ge(CreditRecord::getCreatedAt, windowStart.atStartOfDay())
        );

        // 近7天趋势
        for (int i = 6; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            LocalDateTime dayEnd = date.plusDays(1).atStartOfDay();
            dates.add(date.format(DATE_FMT));

            // 还原"该日终"各用户的信用分：当前分倒扣掉该日之后发生的所有变更。
            // 直接取当前分会让 7 天恒为同一条直线，趋势图失去意义。
            List<Integer> dayScores = allUsers.stream()
                .map(u -> {
                    int current = u.getCreditScore() != null ? u.getCreditScore() : DEFAULT_SCORE;
                    int laterDelta = windowRecords.stream()
                        .filter(r -> u.getId().equals(r.getUserId()))
                        .filter(r -> r.getCreatedAt() != null && !r.getCreatedAt().isBefore(dayEnd))
                        .mapToInt(r -> r.getAmount() != null ? r.getAmount() : 0)
                        .sum();
                    return Math.max(MIN_SCORE, Math.min(MAX_SCORE, current - laterDelta));
                })
                .toList();

            if (dayScores.isEmpty()) {
                avgScores.add((double) DEFAULT_SCORE);
                highScoreUsers.add(0);
                lowScoreUsers.add(0);
                continue;
            }

            double avg = dayScores.stream().mapToInt(Integer::intValue).average().orElse(DEFAULT_SCORE);
            avgScores.add(Math.round(avg * 10) / 10.0);
            highScoreUsers.add((int) dayScores.stream().filter(s -> s >= HIGH_SCORE_THRESHOLD).count());
            lowScoreUsers.add((int) dayScores.stream().filter(s -> s < LOW_SCORE_THRESHOLD).count());
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
