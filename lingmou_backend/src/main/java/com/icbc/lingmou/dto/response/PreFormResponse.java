package com.icbc.lingmou.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 预填单响应DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "预填单信息")
public class PreFormResponse {

    @Schema(description = "预填单ID")
    private Long id;

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "业务类型")
    private String businessType;

    @Schema(description = "原始文本")
    private String rawText;

    @Schema(description = "解析后的JSON")
    private String parsedJson;

    @Schema(description = "状态: DRAFT/SUBMITTED/USED")
    private String status;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;
}
