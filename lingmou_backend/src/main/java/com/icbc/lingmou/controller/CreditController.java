package com.icbc.lingmou.controller;

import com.icbc.lingmou.common.BusinessException;
import com.icbc.lingmou.common.Result;
import com.icbc.lingmou.common.ResultCode;
import com.icbc.lingmou.dto.response.CreditScoreResponse;
import com.icbc.lingmou.service.CreditService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 信用分控制器
 */
@Tag(name = "信用分管理", description = "信用分查询与调整")
@RestController
@RequestMapping("/api/credit")
@RequiredArgsConstructor
@Validated
public class CreditController {

    private final CreditService creditService;

    /** 允许调整信用分的角色（与 API_DAY7 契约一致） */
    private static final List<String> ADJUST_ALLOWED_ROLES = List.of("AUDITOR", "RISK", "ADMIN");

    /**
     * 角色校验：调整信用分属于敏感操作，普通 CUSTOMER 不可调用
     */
    private void assertAdjustAllowed(HttpServletRequest request) {
        String role = (String) request.getAttribute("role");
        if (role == null || !ADJUST_ALLOWED_ROLES.contains(role)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "仅 AUDITOR / RISK / ADMIN 角色可调整信用分");
        }
    }

    @Operation(summary = "查询信用分", description = "查询当前用户的信用分及变更历史")
    @GetMapping
    public Result<CreditScoreResponse> getCreditScore(HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        CreditScoreResponse response = creditService.getCreditScore(userId);
        return Result.success(response);
    }

    @Operation(summary = "调整信用分", description = "管理员调整用户信用分（仅 AUDITOR/RISK/ADMIN 角色可调用）")
    @PostMapping("/adjust")
    public Result<CreditScoreResponse> adjustCredit(
            @RequestParam @NotNull(message = "目标用户ID不能为空") Long targetUserId,
            @RequestParam @NotNull(message = "调整分数不能为空") Integer amount,
            @RequestParam(required = false) String reason,
            HttpServletRequest httpRequest) {
        assertAdjustAllowed(httpRequest);
        Long operatorId = (Long) httpRequest.getAttribute("userId");
        CreditScoreResponse response = creditService.adjustCredit(operatorId, targetUserId, amount, reason);
        return Result.success("信用分调整成功", response);
    }
}
