package com.icbc.lingmou.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 凭证实体类
 */
@Data
@TableName("vouchers")
public class Voucher {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 凭证号 */
    private String voucherNum;

    /** 关联预约ID */
    private Long appointmentId;

    /** 二维码内容 */
    private String qrcodeContent;

    /** 流水号SN */
    private String sn;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    /** 逻辑删除: 0-未删除 1-已删除 */
    @TableLogic
    private Integer deleted;
}
