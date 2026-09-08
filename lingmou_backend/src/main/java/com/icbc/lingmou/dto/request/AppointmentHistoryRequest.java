package com.icbc.lingmou.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

/**
 * 历史预约查询请求DTO
 */
@Data
@Schema(description = "历史预约查询请求")
public class AppointmentHistoryRequest {

    @Schema(description = "网点ID")
    private Long branchId;

    @Schema(description = "业务类型")
    private String businessType;

    @Schema(description = "状态")
    private String status;

    @Schema(description = "开始日期")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate startDate;

    @Schema(description = "结束日期")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate endDate;

    @Schema(description = "页码（从1开始）", example = "1")
    private Integer pageNum = 1;

    @Schema(description = "每页大小", example = "10")
    private Integer pageSize = 10;
}
