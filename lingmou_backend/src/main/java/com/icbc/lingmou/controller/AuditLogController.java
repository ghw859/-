package com.icbc.lingmou.controller;

import com.icbc.lingmou.common.BusinessException;
import com.icbc.lingmou.common.PageResult;
import com.icbc.lingmou.common.Result;
import com.icbc.lingmou.common.ResultCode;
import com.icbc.lingmou.dto.response.AuditLogResponse;
import com.icbc.lingmou.service.AuditLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 审计日志控制器（区块链式Hash链）
 *
 * 访问控制：仅 AUDITOR / RISK / ADMIN 角色可访问
 * 每条记录返回 hashValid 字段，前端可直接展示校验结果
 */
@Tag(name = "审计日志", description = "区块链式操作日志查询（仅 AUDITOR/RISK/ADMIN 可访问）")
@RestController
@RequestMapping("/api/audit/logs")
@RequiredArgsConstructor
public class AuditLogController {

    private final AuditLogService auditLogService;

    private static final List<String> ALLOWED_ROLES = List.of("AUDITOR", "RISK", "ADMIN");

    /**
     * 角色校验：AUDITOR / RISK / ADMIN 通过，其他 403
     */
    private void assertRoleAllowed(HttpServletRequest request) {
        String role = (String) request.getAttribute("role");
        if (role == null || !ALLOWED_ROLES.contains(role)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "仅 AUDITOR / RISK / ADMIN 角色可访问审计日志");
        }
    }

    @Operation(summary = "查询审计日志（分页 + Hash校验）",
            description = "返回每条记录时自动校验 hash 是否自洽（hashValid 字段）。仅 AUDITOR / RISK / ADMIN 角色可访问")
    @GetMapping
    public Result<PageResult<AuditLogResponse>> getLogs(
            @Parameter(description = "操作类型筛选（可选），如 APPOINTMENT_CREATE / VOUCHER_GENERATE")
            @RequestParam(required = false) String action,
            @Parameter(description = "页码（默认 1）")
            @RequestParam(defaultValue = "1") int pageNum,
            @Parameter(description = "每页大小（默认 20）")
            @RequestParam(defaultValue = "20") int pageSize,
            HttpServletRequest httpRequest) {
        assertRoleAllowed(httpRequest);
        PageResult<AuditLogResponse> result = auditLogService.getLogs(action, pageNum, pageSize);
        return Result.success(result);
    }

    @Operation(summary = "校验整条Hash链完整性",
            description = "遍历所有记录验证 prevHash→hash 链是否闭合，未闭合返回 details 含断裂位置")
    @GetMapping("/verify")
    public Result<Map<String, Object>> verifyChain(HttpServletRequest httpRequest) {
        assertRoleAllowed(httpRequest);
        boolean intact = auditLogService.verifyChain();
        return Result.success(Map.of("intact", intact));
    }
}
