package com.icbc.lingmou.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 业务类型饼图响应
 */
@Data
@Builder
@Schema(description = "业务类型分布")
public class BusinessPieResponse {

    @Schema(description = "业务类型分布")
    private List<BusinessItem> items;

    @Schema(description = "业务类型条目")
    @Data
    @Builder
    public static class BusinessItem {
        @Schema(description = "业务类型")
        private String businessType;

        @Schema(description = "数量")
        private Integer count;

        @Schema(description = "占比（%）")
        private Double percentage;
    }
}
