package com.icbc.lingmou.service.impl;

import com.icbc.lingmou.common.BusinessException;
import com.icbc.lingmou.common.ResultCode;
import com.icbc.lingmou.service.FileUploadService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 文件上传服务实现（本地磁盘）
 * URL 访问链路：前端请求 /uploads/2026/09/xxx.jpg → Spring 静态资源映射到 file:./uploads/2026/09/xxx.jpg
 */
@Slf4j
@Service
public class FileUploadServiceImpl implements FileUploadService {

    @Value("${lingmou.upload.dir:./uploads}")
    private String uploadDir;

    @Value("${lingmou.upload.max-size:5242880}")
    private long maxSize;

    private static final DateTimeFormatter YMD = DateTimeFormatter.ofPattern("yyyy/MM");

    @Override
    public UploadResult upload(MultipartFile file, String category) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "上传文件不能为空");
        }
        if (file.getSize() > maxSize) {
            throw new BusinessException(ResultCode.PARAM_ERROR,
                    "文件大小超过上限 " + (maxSize / 1024 / 1024) + "MB");
        }

        String original = file.getOriginalFilename();
        String ext = extractExt(original);
        String saved = UUID.randomUUID().toString().replace("-", "")
                + (ext.isBlank() ? "" : "." + ext);

        // 目录：uploads/{category}/{yyyy}/{MM}/
        String dateDir = LocalDate.now().format(YMD);
        Path relativePath = Paths.get(category, dateDir, saved);
        Path localDir = Paths.get(uploadDir, category, dateDir);
        Path localPath = localDir.resolve(saved);

        try {
            Files.createDirectories(localDir);
            Files.copy(file.getInputStream(), localPath);
        } catch (IOException e) {
            log.error("[文件上传] 保存失败: {}", localPath, e);
            throw new BusinessException(ResultCode.SIGNATURE_UPLOAD_FAIL, "文件保存失败: " + e.getMessage());
        }

        // 相对 URL，供前端访问（静态资源映射把 /uploads/** → file:./uploads/**）
        String url = "/uploads/" + relativePath.toString().replace("\\", "/");

        log.info("[文件上传] {} → {} ({} bytes)", original, url, file.getSize());
        return new UploadResult(
                original, saved, url, file.getSize(), file.getContentType()
        );
    }

    @Override
    public List<UploadResult> uploadBatch(MultipartFile[] files, String category) {
        List<UploadResult> results = new ArrayList<>();
        if (files != null) {
            for (MultipartFile f : files) {
                results.add(upload(f, category));
            }
        }
        return results;
    }

    @Override
    public String resolveLocalPath(String relativeUrl) {
        if (relativeUrl == null || !relativeUrl.startsWith("/uploads/")) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "非法的资源 URL");
        }
        String sub = relativeUrl.substring("/uploads/".length());
        return Paths.get(uploadDir, sub).toAbsolutePath().toString();
    }

    @Override
    public boolean delete(String relativeUrl) {
        String local = resolveLocalPath(relativeUrl);
        File f = new File(local);
        boolean ok = f.exists() && f.delete();
        if (ok) {
            log.info("[文件删除] {}", local);
        }
        return ok;
    }

    private String extractExt(String filename) {
        if (filename == null) return "";
        int idx = filename.lastIndexOf('.');
        return idx >= 0 ? filename.substring(idx + 1).toLowerCase() : "";
    }
}
