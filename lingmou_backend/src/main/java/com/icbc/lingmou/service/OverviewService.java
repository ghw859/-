package com.icbc.lingmou.service;

import com.icbc.lingmou.dto.response.*;

/**
 * 数据总览Service接口
 */
public interface OverviewService {

    /**
     * 获取卡片统计数据（4个指标 + 变化值）
     */
    OverviewCardResponse getOverviewCards();

    /**
     * 获取资产趋势图数据（近7天）
     */
    AssetsChartResponse getAssetsChart();

    /**
     * 获取信用分趋势数据（近7天）
     */
    CreditTrendResponse getCreditTrend();

    /**
     * 获取业务类型饼图分布
     */
    BusinessPieResponse getBusinessPie();
}
