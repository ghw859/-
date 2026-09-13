package com.icbc.lingmou.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.icbc.lingmou.common.PageResult;
import com.icbc.lingmou.dto.response.AuditLogResponse;
import com.icbc.lingmou.dto.response.CustomerAuditLogResponse;
import com.icbc.lingmou.entity.AuditLog;
import com.icbc.lingmou.mapper.AuditLogMapper;
import com.icbc.lingmou.service.AuditLogService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 审计日志Service实现（区块链式Hash链）
 *
 * Hash 公式：SHA256(prevHash + operatorId + operatorName + action + content)
 * 链首 prevHash = 64 个 "0"
 */
@Slf4j
@Service
public class AuditLogServiceImpl implements AuditLogService {

    private final AuditLogMapper auditLogMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String GENESIS_HASH = "0".repeat(64);
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public AuditLogServiceImpl(AuditLogMapper auditLogMapper) {
        this.auditLogMapper = auditLogMapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void writeLog(Long operatorId, String operatorName, String action, String content) {
        // 取最后一条记录的 hash 作为 prevHash
        AuditLog last = auditLogMapper.selectOne(
                new LambdaQueryWrapper<AuditLog>()
                        .orderByDesc(AuditLog::getId)
                        .last("LIMIT 1"));
        String prevHash = (last != null) ? last.getHash() : GENESIS_HASH;

        String rawContent = content == null ? "" : content;
        String hash = sha256Hex(prevHash + operatorId + operatorName + action + rawContent);

        AuditLog auditLog = new AuditLog();
        auditLog.setOperatorId(operatorId);
        auditLog.setOperatorName(operatorName);
        auditLog.setAction(action);
        auditLog.setContent(rawContent);
        auditLog.setPrevHash(prevHash);
        auditLog.setHash(hash);
        auditLogMapper.insert(auditLog);

        log.info("[审计] writeLog action={} operator={} hash={}", action, operatorName, hash.substring(0, 8));
    }

    @Override
    public PageResult<AuditLogResponse> getLogs(String action, int pageNum, int pageSize) {
        LambdaQueryWrapper<AuditLog> wrapper = new LambdaQueryWrapper<>();
        if (action != null && !action.isBlank()) {
            wrapper.eq(AuditLog::getAction, action);
        }
        wrapper.orderByDesc(AuditLog::getId);

        IPage<AuditLog> page = auditLogMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);

        List<AuditLogResponse> records = page.getRecords().stream().map(this::toResponseWithValidation).toList();

        return PageResult.of(pageNum, pageSize, page.getTotal(), records);
    }

    @Override
    public boolean verifyChain() {
        List<AuditLog> all = auditLogMapper.selectList(
                new LambdaQueryWrapper<AuditLog>().orderByAsc(AuditLog::getId));
        for (int i = 0; i < all.size(); i++) {
            AuditLog cur = all.get(i);
            String expectedPrev = (i == 0) ? GENESIS_HASH : all.get(i - 1).getHash();
            String expectedHash = sha256Hex(expectedPrev
                    + cur.getOperatorId()
                    + cur.getOperatorName()
                    + cur.getAction()
                    + (cur.getContent() == null ? "" : cur.getContent()));

            if (!expectedPrev.equals(cur.getPrevHash()) || !expectedHash.equals(cur.getHash())) {
                log.warn("[审计链] 校验失败 id={}", cur.getId());
                return false;
            }
        }
        return true;
    }

    @Override
    public List<CustomerAuditLogResponse> getMyLogs(Long userId) {
        // 强制用户隔离：只取本人名下的直通码存证
        List<AuditLog> logs = auditLogMapper.selectList(
                new LambdaQueryWrapper<AuditLog>()
                        .eq(AuditLog::getOperatorId, userId)
                        .eq(AuditLog::getAction, ACTION_VOUCHER_GENERATE)
                        .orderByDesc(AuditLog::getId));

        List<CustomerAuditLogResponse> result = new ArrayList<>(logs.size());
        for (AuditLog logEntry : logs) {
            result.add(toCustomerResponse(logEntry));
        }
        return result;
    }

    @Override
    public boolean verifyMyChain(Long userId) {
        List<AuditLog> logs = auditLogMapper.selectList(
                new LambdaQueryWrapper<AuditLog>()
                        .eq(AuditLog::getOperatorId, userId)
                        .eq(AuditLog::getAction, ACTION_VOUCHER_GENERATE)
                        .orderByAsc(AuditLog::getId));
        for (AuditLog logEntry : logs) {
            if (!validateHash(logEntry)) {
                log.warn("[审计链] 个人存证校验失败 userId={} id={}", userId, logEntry.getId());
                return false;
            }
        }
        return true;
    }

    /**
     * 审计实体 → 客户存证 DTO：反解 content JSON、PII 脱敏、字段对齐前端 AuditLog 结构
     */
    private CustomerAuditLogResponse toCustomerResponse(AuditLog auditLog) {
        Map<String, Object> content = parseContent(auditLog.getContent());
        boolean hashValid = validateHash(auditLog);

        Map<String, Object> extraData;
        Object ed = content.get("extraData");
        if (ed instanceof Map<?, ?> map) {
            extraData = objectMapper.convertValue(map, new TypeReference<Map<String, Object>>() {});
        } else {
            extraData = new LinkedHashMap<>();
        }

        String idCard = Objects.toString(content.get("idCard"), "");
        String phone = Objects.toString(content.get("phone"), "");

        return CustomerAuditLogResponse.builder()
                .id(auditLog.getId())
                .sn(Objects.toString(content.get("sn"), ""))
                .bizType(Objects.toString(content.get("businessType"), ""))
                .bizTypeName(Objects.toString(content.get("bizTypeName"), "其他业务"))
                .userName(Objects.toString(content.get("userName"), ""))
                .idCardMasked(maskIdCard(idCard))
                .phoneMasked(maskPhone(phone))
                .extraData(extraData)
                .timestamp(auditLog.getCreatedAt() != null ? auditLog.getCreatedAt().format(TIME_FMT) : "")
                .hash(auditLog.getHash())
                .status(Objects.toString(content.getOrDefault("status", "已提交"), "已提交"))
                .voucherNum(Objects.toString(content.get("voucherNum"), ""))
                .hashValid(hashValid)
                .build();
    }

    private Map<String, Object> parseContent(String content) {
        if (content == null || content.isBlank()) {
            return Collections.emptyMap();
        }
        try {
            return objectMapper.readValue(content, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            log.warn("[审计] content JSON 反解失败 id={}: {}", content, e.getMessage());
            return Collections.emptyMap();
        }
    }

    /** 身份证脱敏：前4 + ******** + 后4；过短原样返回 */
    private static String maskIdCard(String idCard) {
        if (idCard == null || idCard.length() < 8) {
            return idCard == null ? "" : idCard;
        }
        return idCard.substring(0, 4) + "********" + idCard.substring(idCard.length() - 4);
    }

    /** 手机号脱敏：前3 + **** + 后4；过短原样返回 */
    private static String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) {
            return phone == null ? "" : phone;
        }
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }

    /**
     * 将 Entity 转为 Response 并校验 hash 是否自洽
     */
    private AuditLogResponse toResponseWithValidation(AuditLog auditLog) {
        boolean valid = validateHash(auditLog);
        return AuditLogResponse.builder()
                .id(auditLog.getId())
                .operatorId(auditLog.getOperatorId())
                .operatorName(auditLog.getOperatorName())
                .action(auditLog.getAction())
                .content(auditLog.getContent())
                .prevHash(auditLog.getPrevHash())
                .hash(auditLog.getHash())
                .hashValid(valid)
                .createdAt(auditLog.getCreatedAt())
                .build();
    }

    /**
     * 校验单条记录的 hash 是否等于 SHA256(prevHash + operatorId + operatorName + action +
     * content)
     */
    private boolean validateHash(AuditLog auditLog) {
        if (auditLog.getHash() == null)
            return false;
        String content = auditLog.getContent() == null ? "" : auditLog.getContent();
        String expected = sha256Hex(auditLog.getPrevHash()
                + auditLog.getOperatorId()
                + auditLog.getOperatorName()
                + auditLog.getAction()
                + content);
        return expected.equals(auditLog.getHash());
    }

    /**
     * SHA-256，返回小写 hex 字符串
     */
    public static String sha256Hex(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] bytes = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("SHA-256 error", e);
        }
    }
}
