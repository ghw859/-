package com.icbc.lingmou.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 信用分变更记录实体类
 */
@Data
@TableName("credit_records")
public class CreditRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    /** 变更类型: ADD/DEDUCT */
    private String changeType;

    /** 变更分数（正负） */
    private Integer amount;

    /** 变更原因 */
    private String reason;

    /** 操作人ID（系统为0） */
    private Long operatorId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /** 逻辑删除: 0-未删除 1-已删除 */
    @TableLogic
    private Integer deleted;
}
