package com.icbc.lingmou.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 网点响应DTO — 逐字对齐前端 branch.ts interface Branch
 *
 * 字段名、类型、枚举值必须与前端完全一致，前端代码冻结
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "网点信息（对齐前端 Branch interface）")
public class BranchResponse {

    @Schema(description = "网点ID（字符串 b1~b6）", example = "b1")
    private String id;

    @Schema(description = "网点名称", example = "北京分行营业部")
    private String name;

    @Schema(description = "繁忙程度: free/moderate/busy", example = "busy")
    private String status;

    @Schema(description = "距离(米)", example = "850")
    private Integer distance;

    @Schema(description = "业务标签数组", example = "[\"大额现金\",\"外汇\",\"无障碍\"]")
    private List<String> services;

    @Schema(description = "等待时间(分钟)", example = "25")
    private Integer wait;

    @Schema(description = "在店人数", example = "42")
    private Integer flow;

    @Schema(description = "线上预约人数", example = "18")
    private Integer reserve;

    @Schema(description = "窗口信息如 8/10", example = "8/10")
    private String window;

    @Schema(description = "人流趋势数组", example = "[18,22,28,20,25,25]")
    private List<Integer> trend;

    @Schema(description = "网点地址", example = "北京市西城区复兴门内大街55号")
    private String address;

    @Schema(description = "联系电话", example = "010-66695588")
    private String phone;

    @Schema(description = "营业时间", example = "09:00 - 17:00")
    private String hours;

    @Schema(description = "图标class", example = "fa-solid fa-landmark")
    private String icon;

    @Schema(description = "图标背景class", example = "bg-blue-50")
    private String iconBg;

    @Schema(description = "图标颜色class", example = "text-blue-600")
    private String iconColor;

    @Schema(description = "是否收藏", example = "false")
    private Boolean favorite;
}
