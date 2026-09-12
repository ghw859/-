package com.icbc.lingmou.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

/**
 * 预约请求DTO
 */
@Data
@Schema(description = "创建预约请求")
public class AppointmentRequest {

    @Schema(description = "网点ID", example = "1")
    @NotNull(message = "网点ID不能为空")
    private Long branchId;

    @Schema(description = "业务类型", example = "开户")
    @NotBlank(message = "业务类型不能为空")
    private String businessType;

    @Schema(description = "预约日期（今天或之后）", example = "2026-09-10")
    @NotNull(message = "预约日期不能为空")
    @FutureOrPresent(message = "预约日期不能早于今天")
    private LocalDate appointmentDate;

    @Schema(description = "时段，如 09:00-09:30", example = "09:00-09:30")
    @NotBlank(message = "时段不能为空")
    private String timeSlot;
}
