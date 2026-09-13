package com.icbc.lingmou.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.Map;

/**
 * 业务直通码生成请求
 */
@Data
@Schema(description = "业务直通码生成请求")
public class QRCodeGenerateRequest {

    @NotBlank(message = "业务类型不能为空")
    @Schema(description = "业务类型，前端 5 枚举（cash_reserve/open_card/corp_transfer/cash_deposit/fx_exchange）"
            + "或标准枚举 OPEN_ACCOUNT/TRANSFER/LOAN 等", example = "cash_reserve")
    private String businessType;

    @Schema(description = "提交的材料字段字典（前端表单原始键名，后端负责归一化给 AI 预检）",
            example = "{\"idCard\":\"110101199001011234\",\"phone\":\"13800138000\",\"address\":\"北京市...\"}")
    private Map<String, Object> materials;

    @Schema(description = "网点ID（用于生成凭证号的网点编码）", example = "1")
    private Long branchId;

    @Schema(description = "关联预约ID（预约后补开直通码时传入，写入 vouchers.appointment_id）", example = "12")
    private Long appointmentId;

    @Schema(description = "关联预填单ID（预填单→直通码链路传入，随审计内容落链）", example = "3")
    private Long preFormId;
}
