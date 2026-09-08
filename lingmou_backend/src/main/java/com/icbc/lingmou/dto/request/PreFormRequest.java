package com.icbc.lingmou.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 预填单请求DTO
 */
@Data
@Schema(description = "预填单请求")
public class PreFormRequest {

    @Schema(description = "业务类型", example = "开户")
    @NotBlank(message = "业务类型不能为空")
    private String businessType;

    @Schema(description = "原始文本内容", example = "姓名：张三\n身份证号：110101...")
    @NotBlank(message = "原始文本不能为空")
    private String rawText;

    @Schema(description = "解析后的JSON（可选）")
    private String parsedJson;
}
