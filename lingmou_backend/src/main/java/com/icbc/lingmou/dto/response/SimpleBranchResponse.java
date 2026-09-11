package com.icbc.lingmou.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 网点简化响应DTO（simple=true 时返回）
 * 字段名与前端 Branch interface 子集一致
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "网点简化信息（状态概览）")
public class SimpleBranchResponse {

    @Schema(description = "网点ID（字符串 b1~b6）", example = "b1")
    private String id;

    @Schema(description = "网点名称")
    private String name;

    @Schema(description = "繁忙程度: free/moderate/busy")
    private String status;

    @Schema(description = "距离(米)")
    private Integer distance;

    @Schema(description = "等待时间(分钟)")
    private Integer wait;

    @Schema(description = "在店人数")
    private Integer flow;

    @Schema(description = "窗口信息如 8/10")
    private String window;
}
