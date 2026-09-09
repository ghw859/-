package com.icbc.lingmou.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 资产趋势图响应
 */
@Data
@Builder
@Schema(description = "资产趋势图数据")
public class AssetsChartResponse {

    @Schema(description = "日期列表")
    private List<String> dates;

    @Schema(description = "开户数列表")
    private List<Integer> accounts;

    @Schema(description = "充值金额列表")
    private List<Long> deposits;

    @Schema(description = "理财金额列表")
    private List<Long> finances;
}
