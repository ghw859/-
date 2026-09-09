package com.icbc.lingmou.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 信用分响应
 */
@Data
@Builder
@Schema(description = "信用分详情")
public class CreditScoreResponse {

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "用户名")
    private String username;

    @Schema(description = "当前信用分")
    private Integer creditScore;

    @Schema(description = "信用等级: EXCELLENT(>=90)/GOOD(>=75)/FAIR(>=60)/POOR(<60)")
    private String creditLevel;

    @Schema(description = "客户级别: NORMAL/SILVER/GOLD")
    private String customerLevel;

    @Schema(description = "信用分变更记录")
    private List<CreditChangeItem> changeHistory;

    @Schema(description = "信用分变更条目")
    @Data
    @Builder
    public static class CreditChangeItem {
        @Schema(description = "变更类型: ADD/DEDUCT")
        private String changeType;

        @Schema(description = "变更分数（正负）")
        private Integer amount;

        @Schema(description = "变更原因")
        private String reason;

        @Schema(description = "变更时间")
        private LocalDateTime createdAt;
    }
}
