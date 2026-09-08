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
    @Schema(description = "业务类型，如 OPEN_ACCOUNT / TRANSFER / LOAN", example = "OPEN_ACCOUNT")
    private String businessType;

    @Schema(description = "提交的材料字段字典", example = "{\"idCard\":\"110101199001011234\",\"phone\":\"13800138000\"}")
    private Map<String, Object> materials;

    @Schema(description = "网点ID（用于生成凭证号的网点编码）", example = "1")
    private Long branchId;
}
