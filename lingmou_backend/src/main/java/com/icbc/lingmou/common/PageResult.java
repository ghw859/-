package com.icbc.lingmou.common;

import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 通用分页结果容器
 * 放在 common 目录，所有需要分页的接口统一使用
 *
 * @param <T> 数据记录类型
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "分页结果")
public class PageResult<T> {

    @Schema(description = "当前页码（从1开始）")
    private Integer pageNum;

    @Schema(description = "每页大小")
    private Integer pageSize;

    @Schema(description = "总记录数")
    private Long total;

    @Schema(description = "总页数")
    private Integer totalPages;

    @Schema(description = "当前页数据列表")
    private List<T> records;

    /**
     * 从 MyBatis-Plus IPage 转换
     */
    public static <T> PageResult<T> of(IPage<T> page) {
        int totalPages = (int) Math.ceil((double) page.getTotal() / page.getSize());
        return PageResult.<T>builder()
                .pageNum((int) page.getCurrent())
                .pageSize((int) page.getSize())
                .total(page.getTotal())
                .totalPages(totalPages)
                .records(page.getRecords())
                .build();
    }

    /**
     * 手动构建
     */
    public static <T> PageResult<T> of(int pageNum, int pageSize, long total, List<T> records) {
        int totalPages = (int) Math.ceil((double) total / pageSize);
        return PageResult.<T>builder()
                .pageNum(pageNum)
                .pageSize(pageSize)
                .total(total)
                .totalPages(totalPages)
                .records(records)
                .build();
    }
}
