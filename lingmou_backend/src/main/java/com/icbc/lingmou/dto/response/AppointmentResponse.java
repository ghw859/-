package com.icbc.lingmou.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 预约响应DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "预约信息")
public class AppointmentResponse {

    @Schema(description = "预约ID")
    private Long id;

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "网点ID")
    private Long branchId;

    @Schema(description = "网点名称")
    private String branchName;

    @Schema(description = "业务类型")
    private String businessType;

    @Schema(description = "预约日期")
    private LocalDate appointmentDate;

    @Schema(description = "时段")
    private String timeSlot;

    @Schema(description = "排队号")
    private String queueNumber;

    @Schema(description = "状态: VIRTUAL/ACTIVE/CALLED/PROCESSING/COMPLETED/EXPIRED/CANCELED")
    private String status;

    @Schema(description = "凭证号")
    private String voucherNum;

    @Schema(description = "进度步骤: 0-取号 1-排队 2-叫号 3-办理 4-完成")
    private Integer progressStep;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;
}
