package com.icbc.lingmou.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 审计日志实体类（区块链式）
 */
@Data
@TableName("audit_logs")
public class AuditLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 操作人ID */
    private Long operatorId;

    /** 操作人姓名 */
    private String operatorName;

    /** 操作类型 */
    private String action;

    /** 操作内容 */
    private String content;

    /** 前一条记录的hash */
    private String prevHash;

    /** 当前记录的hash */
    private String hash;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /** 逻辑删除: 0-未删除 1-已删除 */
    @TableLogic
    private Integer deleted;
}
