package com.icbc.lingmou.service;

import com.icbc.lingmou.dto.response.CreditScoreResponse;

/**
 * 信用分Service接口
 */
public interface CreditService {

    /**
     * 获取用户信用分详情
     */
    CreditScoreResponse getCreditScore(Long userId);

    /**
     * 调整用户信用分
     */
    CreditScoreResponse adjustCredit(Long operatorId, Long targetUserId, Integer amount, String reason);
}
