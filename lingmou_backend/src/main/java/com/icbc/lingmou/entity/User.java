package com.icbc.lingmou.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 用户实体类
 */
@Data
@TableName("users")
public class User {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String username;

    private String password;

    private String realName;

    private String idCard;

    private String phone;

    /** 用户头像 URL */
    private String avatar;

    /** 角色: CUSTOMER/AUDITOR/RISK/ADMIN */
    private String role;

    /** 客户级别: NORMAL/SILVER/GOLD */
    private String customerLevel;

    /** 信用分，默认100 */
    private Integer creditScore;

    /** 老年模式: 0-关闭 1-开启 */
    private Integer elderlyMode;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    /** 逻辑删除: 0-未删除 1-已删除 */
    @TableLogic
    private Integer deleted;
}
