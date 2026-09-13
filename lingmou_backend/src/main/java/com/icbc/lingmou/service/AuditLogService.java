package com.icbc.lingmou.service;

import com.icbc.lingmou.common.PageResult;
import com.icbc.lingmou.dto.response.AuditLogResponse;
import com.icbc.lingmou.dto.response.CustomerAuditLogResponse;

import java.util.List;

/**
 * 审计日志Service（区块链式Hash链）
 */
public interface AuditLogService {

    /** 业务直通码生成（T3 全链路客户侧存证动作） */
    String ACTION_VOUCHER_GENERATE = "VOUCHER_GENERATE";

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

    /**
     * 查询当前用户的存证记录（业务直通码生成记录，按用户隔离）。
     * data 直接为数组（前端冻结结构）。
     */
    List<CustomerAuditLogResponse> getMyLogs(Long userId);

    /**
     * 校验当前用户名下存证记录的 hash 是否逐条自洽。
     */
    boolean verifyMyChain(Long userId);
}
