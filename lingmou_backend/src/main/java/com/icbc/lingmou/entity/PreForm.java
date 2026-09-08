package com.icbc.lingmou.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 预填单实体类
 */
@Data
@TableName("pre_forms")
public class PreForm {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    /** 业务类型 */
    private String businessType;

    /** 原始文本 */
    private String rawText;

    /** 解析后的JSON */
    private String parsedJson;

    /** 上传的材料图片URL列表（JSON数组字符串） */
    private String imageUrls;

    /** 签名图片 URL */
    private String signatureUrl;

    /** 状态: DRAFT/SUBMITTED/USED */
    private String status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    /** 逻辑删除: 0-未删除 1-已删除 */
    @TableLogic
    private Integer deleted;
}
