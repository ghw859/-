package com.icbc.lingmou.service.impl;

import com.icbc.lingmou.common.BusinessException;
import com.icbc.lingmou.common.ResultCode;
import com.icbc.lingmou.service.AiPrecheckService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

/**
 * AI 预检服务实现
 * 调用 Python FastAPI 的 POST /api/ai/precheck
 */
@Slf4j
@Service
public class AiPrecheckServiceImpl implements AiPrecheckService {

    private final RestTemplate restTemplate;

    @Value("${lingmou.ai.base-url:http://localhost:8000}")
    private String aiBaseUrl;

    public AiPrecheckServiceImpl(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Override
    public PrecheckResult precheck(String businessType, Map<String, Object> materials) {
        String url = aiBaseUrl + "/api/ai/precheck";

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("businessType", businessType);
        body.put("materials", materials != null ? materials : new HashMap<>());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        log.info("[AI预检] 调用 Python: businessType={}, url={}", businessType, url);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, entity, Map.class);

            if (response.getStatusCode() != HttpStatus.OK || response.getBody() == null) {
                log.error("[AI预检] HTTP 非200: {}", response.getStatusCode());
                throw new BusinessException(ResultCode.AI_SERVICE_ERROR, "AI预检服务返回异常");
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> respBody = response.getBody();
            Number code = (Number) respBody.get("code");
            Object data = respBody.get("data");

            if (code == null || code.intValue() != 0) {
                String msg = String.valueOf(respBody.getOrDefault("msg", "AI预检失败"));
                log.warn("[AI预检] Python 返回错误: code={}, msg={}", code, msg);

                // 根据错误码细分
                if (code != null && code.intValue() == 60004) {
                    throw new BusinessException(ResultCode.AI_PRECHECK_UNKNOWN_BUSINESS, msg);
                }
                throw new BusinessException(ResultCode.AI_PRECHECK_REJECTED, msg);
            }

            // 解析 data
            boolean passed = false;
            List<String> missing = new ArrayList<>();
            List<String> required = new ArrayList<>();

            if (data instanceof Map<?, ?> d) {
                Object p = d.get("passed");
                if (p instanceof Boolean b)
                    passed = b;

                Object m = d.get("missing");
                if (m instanceof List<?> list) {
                    for (Object item : list)
                        missing.add(String.valueOf(item));
                }

                Object r = d.get("required");
                if (r instanceof List<?> list) {
                    for (Object item : list)
                        required.add(String.valueOf(item));
                }
            }

            log.info("[AI预检] 通过={}, missing={}", passed, missing);
            return new PrecheckResult(passed, missing, required, respBody);

        } catch (BusinessException be) {
            throw be;
        } catch (Exception e) {
            log.error("[AI预检] 调用 Python 失败: ", e);
            throw new BusinessException(ResultCode.AI_SERVICE_ERROR,
                    "AI预检服务调用失败: " + e.getMessage());
        }
    }
}
