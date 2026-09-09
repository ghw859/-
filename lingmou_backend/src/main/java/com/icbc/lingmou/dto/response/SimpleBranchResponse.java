package com.icbc.lingmou.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 网点简化响应DTO（simple=true 时返回：网点状态概览）
 * 只保留前端列表展示必要的字段
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "网点简化信息（状态概览）")
public class SimpleBranchResponse {

    @Schema(description = "网点ID")
    private Long id;

    @Schema(description = "网点编码：BJ01/SH01/SZ01/HZ01")
    private String branchCode;

    @Schema(description = "网点名称")
    private String name;

    @Schema(description = "繁忙程度: IDLE/MODERATE/BUSY")
    private String busyLevel;

    @Schema(description = "当前排队人数")
    private Integer currentQueue;
}
