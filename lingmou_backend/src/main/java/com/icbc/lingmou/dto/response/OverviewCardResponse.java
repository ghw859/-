package com.icbc.lingmou.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

/**
 * 数据总览-卡片统计响应
 */
@Data
@Builder
@Schema(description = "数据总览卡片统计")
public class OverviewCardResponse {

    @Schema(description = "总用户数")
    private Long totalUsers;

    @Schema(description = "今日预约数")
    private Long todayAppointments;

    @Schema(description = "总预填单数")
    private Long totalPreForms;

    @Schema(description = "本月办理完成数")
    private Long monthlyCompleted;

    @Schema(description = "用户数较昨日变化")
    private Integer usersChange;

    @Schema(description = "预约数较昨日变化")
    private Integer appointmentsChange;

    @Schema(description = "预填单数较昨日变化")
    private Integer preFormsChange;

    @Schema(description = "办理完成数较昨日变化")
    private Integer completedChange;
}
