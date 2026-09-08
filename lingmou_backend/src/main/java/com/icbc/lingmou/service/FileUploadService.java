package com.icbc.lingmou.service;

import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 文件上传服务
 * 存储策略：本地 ./uploads/{yyyy}/{mm}/{filename}，返回 /uploads/{...} 形式的 URL 前缀
 */
public interface FileUploadService {

    /**
     * 上传单个文件
     *
     * @param file 上传的文件
     * @param category 业务分类目录，如 avatar / signature / material / qrcode
     * @return 上传结果（含相对 URL）
     */
    UploadResult upload(MultipartFile file, String category);

    /**
     * 批量上传
     */
    List<UploadResult> uploadBatch(MultipartFile[] files, String category);

    /**
     * 根据相对 URL 返回本地绝对路径
     */
    String resolveLocalPath(String relativeUrl);

    /**
     * 删除文件
     */
    boolean delete(String relativeUrl);

    /**
     * 上传结果
     */
    record UploadResult(
            String originalFilename,
            String savedFilename,
            String url,           // 相对前端可访问 URL，如 /uploads/2026/09/xxx.jpg
            long size,
            String contentType
    ) {}
}
