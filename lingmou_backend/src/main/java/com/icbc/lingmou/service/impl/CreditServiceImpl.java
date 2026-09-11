package com.icbc.lingmou.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.icbc.lingmou.common.BusinessException;
import com.icbc.lingmou.common.ResultCode;
import com.icbc.lingmou.dto.response.CreditScoreResponse;
import com.icbc.lingmou.entity.CreditRecord;
import com.icbc.lingmou.entity.User;
import com.icbc.lingmou.mapper.CreditRecordMapper;
import com.icbc.lingmou.mapper.UserMapper;
import com.icbc.lingmou.service.CreditService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 信用分Service实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CreditServiceImpl implements CreditService {

    private final UserMapper userMapper;
    private final CreditRecordMapper creditRecordMapper;

    /** 信用分阈值常量 */
    private static final int EXCELLENT_THRESHOLD = 90;
    private static final int GOOD_THRESHOLD = 75;
    private static final int FAIR_THRESHOLD = 60;
    private static final int MIN_SCORE = 0;
    private static final int MAX_SCORE = 100;

    @Override
    public CreditScoreResponse getCreditScore(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_EXIST);
        }

        Integer score = user.getCreditScore() != null ? user.getCreditScore() : 100;

        // 查询近10条变更记录
        List<CreditRecord> records = creditRecordMapper.selectList(
            new LambdaQueryWrapper<CreditRecord>()
                .eq(CreditRecord::getUserId, userId)
                .orderByDesc(CreditRecord::getCreatedAt)
                .last("LIMIT 10")
        );

        List<CreditScoreResponse.CreditChangeItem> history = records.stream()
            .map(r -> CreditScoreResponse.CreditChangeItem.builder()
                .changeType(r.getChangeType())
                .amount(r.getAmount())
                .reason(r.getReason())
                .createdAt(r.getCreatedAt())
                .build())
            .collect(Collectors.toList());

        return CreditScoreResponse.builder()
            .userId(userId)
            .username(user.getUsername())
            .creditScore(score)
            .creditLevel(calcCreditLevel(score))
            .customerLevel(user.getCustomerLevel())
            .changeHistory(history)
            .build();
    }

    @Override
    @Transactional
    public CreditScoreResponse adjustCredit(Long operatorId, Long targetUserId, Integer amount, String reason) {
        User user = userMapper.selectById(targetUserId);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_EXIST);
        }

        Integer currentScore = user.getCreditScore() != null ? user.getCreditScore() : 100;
        String changeType = amount >= 0 ? "ADD" : "DEDUCT";

        // 计算新信用分，限制在[0, 100]
        int newScore = Math.max(MIN_SCORE, Math.min(MAX_SCORE, currentScore + amount));

        // 记录变更
        CreditRecord record = new CreditRecord();
        record.setUserId(targetUserId);
        record.setChangeType(changeType);
        record.setAmount(amount);
        record.setReason(reason);
        record.setOperatorId(operatorId);
        creditRecordMapper.insert(record);

        // 更新用户信用分
        user.setCreditScore(newScore);
        userMapper.updateById(user);

        log.info("信用分调整: userId={}, operator={}, amount={}, reason={}, {}->{}",
            targetUserId, operatorId, amount, reason, currentScore, newScore);

        return getCreditScore(targetUserId);
    }

    /**
     * 计算信用等级
     */
    private String calcCreditLevel(Integer score) {
        if (score == null) return "FAIR";
        if (score >= EXCELLENT_THRESHOLD) return "EXCELLENT";
        if (score >= GOOD_THRESHOLD) return "GOOD";
        if (score >= FAIR_THRESHOLD) return "FAIR";
        return "POOR";
    }

    @Override
    @Transactional
    public void autoAdjust(Long userId, Integer amount, String reason) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            log.warn("autoAdjust: 用户不存在, userId={}", userId);
            return;
        }

        Integer currentScore = user.getCreditScore() != null ? user.getCreditScore() : 100;
        String changeType = amount >= 0 ? "ADD" : "DEDUCT";
        int newScore = Math.max(MIN_SCORE, Math.min(MAX_SCORE, currentScore + amount));

        CreditRecord record = new CreditRecord();
        record.setUserId(userId);
        record.setChangeType(changeType);
        record.setAmount(amount);
        record.setReason(reason);
        record.setOperatorId(0L); // 0 = 系统自动
        creditRecordMapper.insert(record);

        user.setCreditScore(newScore);
        userMapper.updateById(user);

        log.info("信用分自动调整: userId={}, amount={}, reason={}, {}->{}",
            userId, amount, reason, currentScore, newScore);
    }
}
