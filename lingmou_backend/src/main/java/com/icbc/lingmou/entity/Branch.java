package com.icbc.lingmou.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 网点实体类（DB 列名 ↔ 前端 branch.ts 字段名映射见 BranchResponse）
 */
@Data
@TableName("branches")
public class Branch {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 网点编码 b1~b6（前端 id） */
    private String branchCode;

    private String name;

    private String address;

    /** 联系电话 */
    private String phone;

    /** 营业时间，如 09:00 - 17:00 */
    private String hours;

    /** 距离(米) */
    private Integer distance;

    /** 业务标签数组JSON，如 ["大额现金","外汇"] */
    private String services;

    /** 等待时间(分钟) */
    private Integer waitTime;

    /** 在店人数 */
    private Integer flowCount;

    /** 线上预约人数 */
    private Integer reserveCount;

    /** 窗口信息如 8/10 */
    private String windowInfo;

    /** 人流趋势数组JSON如 [18,22,28] */
    private String trend;

    /** 繁忙程度: free/moderate/busy */
    private String busyLevel;

    /** 网点封面图 URL */
    private String coverImage;

    /** 图标class如 fa-solid fa-landmark */
    private String icon;

    /** 图标背景class如 bg-blue-50 */
    private String iconBg;

    /** 图标颜色class如 text-blue-600 */
    private String iconColor;

    /** 收藏: 0-否 1-是 */
    private Integer favorite;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    /** 逻辑删除: 0-未删除 1-已删除 */
    @TableLogic
    private Integer deleted;
}
