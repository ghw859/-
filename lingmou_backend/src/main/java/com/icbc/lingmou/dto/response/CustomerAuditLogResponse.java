package com.icbc.lingmou.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * 客户侧「我的存证记录」响应 DTO。
 *
 * 字段形态对齐前端冻结的 AuditLog 接口（gonghang_vue3/src/stores/auditLog.ts），
 * 数据来源为 audit_logs 哈希链中当前用户的 VOUCHER_GENERATE 记录（content JSON 反解）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "客户存证记录（业务直通码上链凭证）")
public class CustomerAuditLogResponse {

    @Schema(description = "审计链记录ID")
    private Long id;

    @Schema(description = "业务流水号 SN")
    private String sn;

    @Schema(description = "业务类型（前端枚举，如 cash_reserve）")
    private String bizType;

    @Schema(description = "业务类型中文名")
    private String bizTypeName;

    @Schema(description = "客户姓名")
    private String userName;

    @Schema(description = "身份证号（脱敏）")
    private String idCardMasked;

    @Schema(description = "手机号（脱敏）")
    private String phoneMasked;

    @Schema(description = "业务专项要素")
    private Map<String, Object> extraData;

    @Schema(description = "提交时间（yyyy-MM-dd HH:mm:ss）")
    private String timestamp;

    @Schema(description = "区块链存证哈希（SHA-256）")
    private String hash;

    @Schema(description = "办理状态：已提交/已完成/已取消")
    private String status;

    @Schema(description = "业务直通码凭证号（T/V）")
    private String voucherNum;

    @Schema(description = "本条 hash 是否自洽")
    private Boolean hashValid;
}
