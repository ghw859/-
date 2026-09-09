package com.icbc.lingmou.controller;

import com.icbc.lingmou.common.Result;
import com.icbc.lingmou.dto.response.*;
import com.icbc.lingmou.service.OverviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 数据总览控制器
 */
@Tag(name = "数据总览", description = "数据统计与可视化")
@RestController
@RequestMapping("/api/overview")
@RequiredArgsConstructor
public class OverviewController {

    private final OverviewService overviewService;

    @Operation(summary = "卡片统计", description = "获取4个核心指标的统计数据")
    @GetMapping
    public Result<OverviewCardResponse> getOverviewCards() {
        OverviewCardResponse response = overviewService.getOverviewCards();
        return Result.success(response);
    }

    @Operation(summary = "资产趋势图", description = "获取近7天开户数、充值金额、理财金额趋势")
    @GetMapping("/assets-chart")
    public Result<AssetsChartResponse> getAssetsChart() {
        AssetsChartResponse response = overviewService.getAssetsChart();
        return Result.success(response);
    }

    @Operation(summary = "信用趋势", description = "获取近7天信用分平均分、高分/低分用户数趋势")
    @GetMapping("/credit-trend")
    public Result<CreditTrendResponse> getCreditTrend() {
        CreditTrendResponse response = overviewService.getCreditTrend();
        return Result.success(response);
    }

    @Operation(summary = "业务类型饼图", description = "获取各业务类型的预约数量占比")
    @GetMapping("/business-pie")
    public Result<BusinessPieResponse> getBusinessPie() {
        BusinessPieResponse response = overviewService.getBusinessPie();
        return Result.success(response);
    }
}
