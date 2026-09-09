package com.icbc.lingmou.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 信用趋势响应
 */
@Data
@Builder
@Schema(description = "信用分趋势数据")
public class CreditTrendResponse {

    @Schema(description = "日期列表")
    private List<String> dates;

    @Schema(description = "平均信用分列表")
    private List<Double> avgScores;

    @Schema(description = "高分用户数（>=90）")
    private List<Integer> highScoreUsers;

    @Schema(description = "低分用户数（<70）")
    private List<Integer> lowScoreUsers;
}
