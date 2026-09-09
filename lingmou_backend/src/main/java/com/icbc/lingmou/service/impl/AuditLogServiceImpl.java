package com.icbc.lingmou.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.icbc.lingmou.common.PageResult;
import com.icbc.lingmou.dto.response.AuditLogResponse;
import com.icbc.lingmou.entity.AuditLog;
import com.icbc.lingmou.mapper.AuditLogMapper;
import com.icbc.lingmou.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;

/**
 * 审计日志Service实现（区块链式Hash链）
 *
 * Hash 公式：SHA256(prevHash + operatorId + operatorName + action + content)
 * 链首 prevHash = 64 个 "0"
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuditLogServiceImpl implements AuditLogService {

    private final AuditLogMapper auditLogMapper;

    private static final String GENESIS_HASH = "0".repeat(64);

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void writeLog(Long operatorId, String operatorName, String action, String content) {
        // 取最后一条记录的 hash 作为 prevHash
        AuditLog last = auditLogMapper.selectOne(
                new LambdaQueryWrapper<AuditLog>()
                        .orderByDesc(AuditLog::getId)
                        .last("LIMIT 1")
        );
        String prevHash = (last != null) ? last.getHash() : GENESIS_HASH;

        String rawContent = content == null ? "" : content;
        String hash = sha256Hex(prevHash + operatorId + operatorName + action + rawContent);

        AuditLog log = new AuditLog();
        log.setOperatorId(operatorId);
        log.setOperatorName(operatorName);
        log.setAction(action);
        log.setContent(rawContent);
        log.setPrevHash(prevHash);
        log.setHash(hash);
        auditLogMapper.insert(log);

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
                new LambdaQueryWrapper<AuditLog>().orderByAsc(AuditLog::getId)
        );
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

    /**
     * 将 Entity 转为 Response 并校验 hash 是否自洽
     */
    private AuditLogResponse toResponseWithValidation(AuditLog log) {
        boolean valid = validateHash(log);
        return AuditLogResponse.builder()
                .id(log.getId())
                .operatorId(log.getOperatorId())
                .operatorName(log.getOperatorName())
                .action(log.getAction())
                .content(log.getContent())
                .prevHash(log.getPrevHash())
                .hash(log.getHash())
                .hashValid(valid)
                .createdAt(log.getCreatedAt())
                .build();
    }

    /**
     * 校验单条记录的 hash 是否等于 SHA256(prevHash + operatorId + operatorName + action + content)
     */
    private boolean validateHash(AuditLog log) {
        if (log.getHash() == null) return false;
        String content = log.getContent() == null ? "" : log.getContent();
        String expected = sha256Hex(log.getPrevHash()
                + log.getOperatorId()
                + log.getOperatorName()
                + log.getAction()
                + content);
        return expected.equals(log.getHash());
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
