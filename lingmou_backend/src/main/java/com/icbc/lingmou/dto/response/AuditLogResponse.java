package com.icbc.lingmou.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 审计日志响应DTO（区块链式）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "审计日志（带Hash链校验）")
public class AuditLogResponse {

    @Schema(description = "日志ID")
    private Long id;

    @Schema(description = "操作人ID")
    private Long operatorId;

    @Schema(description = "操作人姓名")
    private String operatorName;

    @Schema(description = "操作类型")
    private String action;

    @Schema(description = "操作内容")
    private String content;

    @Schema(description = "前一条记录的hash")
    private String prevHash;

    @Schema(description = "当前记录的hash")
    private String hash;

    @Schema(description = "Hash是否校验通过（链是否闭合）")
    private Boolean hashValid;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;
}
