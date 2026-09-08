package com.icbc.lingmou.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 预约实体类
 */
@Data
@TableName("appointments")
public class Appointment {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long branchId;

    /** 业务类型 */
    private String businessType;

    /** 预约日期 */
    private LocalDate appointmentDate;

    /** 时段，如 09:00-09:30 */
    private String timeSlot;

    /** 排队号 */
    private String queueNumber;

    /** 状态: VIRTUAL/ACTIVE/CALLED/PROCESSING/COMPLETED/EXPIRED/CANCELED */
    private String status;

    /** 凭证号 */
    private String voucherNum;

    /** 进度步骤: 0-取号 1-排队 2-叫号 3-办理 4-完成 */
    private Integer progressStep;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    /** 逻辑删除: 0-未删除 1-已删除 */
    @TableLogic
    private Integer deleted;
}
