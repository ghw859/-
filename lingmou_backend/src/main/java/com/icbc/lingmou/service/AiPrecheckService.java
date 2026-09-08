package com.icbc.lingmou.service;

import java.util.List;
import java.util.Map;

/**
 * AI 预检服务（调用 Python FastAPI /api/ai/precheck）
 */
public interface AiPrecheckService {

    /**
     * 调用 Python AI 做材料预检
     *
     * @param businessType 业务类型
     * @param materials     提交的材料字段
     * @return 预检结果
     */
    PrecheckResult precheck(String businessType, Map<String, Object> materials);

    /**
     * 预检结果
     */
    record PrecheckResult(
            boolean passed,
            List<String> missing,
            List<String> required,
            Map<String, Object> rawResponse
    ) {}
}
