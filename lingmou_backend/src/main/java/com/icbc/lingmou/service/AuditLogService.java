package com.icbc.lingmou.service;

import com.icbc.lingmou.common.PageResult;
import com.icbc.lingmou.dto.response.AuditLogResponse;

/**
 * 审计日志Service（区块链式Hash链）
 */
public interface AuditLogService {

    /**
     * 写入一条审计日志（自动计算 prevHash + hash）
     */
    void writeLog(Long operatorId, String operatorName, String action, String content);

    /**
     * 分页查询审计日志（带Hash校验）
     */
    PageResult<AuditLogResponse> getLogs(String action, int pageNum, int pageSize);

    /**
     * 校验整条Hash链是否完整（有断链则返回校验失败详情）
     */
    boolean verifyChain();
}
