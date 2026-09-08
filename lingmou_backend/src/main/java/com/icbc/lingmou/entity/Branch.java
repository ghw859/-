package com.icbc.lingmou.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 网点实体类
 */
@Data
@TableName("branches")
public class Branch {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    private String address;

    /** 营业时间，如 09:00-17:00 */
    private String businessHours;

    /** 当前排队人数 */
    private Integer currentQueue;

    /** 繁忙程度: IDLE/MODERATE/BUSY */
    private String busyLevel;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    /** 逻辑删除: 0-未删除 1-已删除 */
    @TableLogic
    private Integer deleted;
}
