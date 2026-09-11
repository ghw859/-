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
     * 调整用户信用分（管理员手动调用）
     */
    CreditScoreResponse adjustCredit(Long operatorId, Long targetUserId, Integer amount, String reason);

    /**
     * 系统自动调整信用分（业务规则触发，operatorId=0）
     */
    void autoAdjust(Long userId, Integer amount, String reason);
}
