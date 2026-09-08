package com.icbc.lingmou.controller;

import com.icbc.lingmou.common.Result;
import com.icbc.lingmou.service.FileUploadService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 文件上传控制器
 * 支持材料图片、签名图片、用户头像等上传
 */
@Tag(name = "文件上传", description = "上传图片/签名/材料等文件")
@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileUploadController {

    private final FileUploadService fileUploadService;

    @Operation(summary = "上传单个文件",
            description = "上传图片/PDF 文件，category 可选：avatar / signature / material / qrcode")
    @PostMapping("/upload")
    public Result<FileUploadService.UploadResult> upload(
            @Parameter(description = "上传文件（MultipartFile）")
            @RequestParam("file") MultipartFile file,
            @Parameter(description = "业务分类目录", example = "material")
            @RequestParam(value = "category", defaultValue = "material") String category) {

        FileUploadService.UploadResult result = fileUploadService.upload(file, category);
        return Result.success("上传成功", result);
    }

    @Operation(summary = "批量上传", description = "一次上传多张图片（材料图片）")
    @PostMapping("/upload/batch")
    public Result<List<FileUploadService.UploadResult>> uploadBatch(
            @Parameter(description = "文件数组")
            @RequestParam("files") MultipartFile[] files,
            @Parameter(description = "业务分类目录", example = "material")
            @RequestParam(value = "category", defaultValue = "material") String category) {

        List<FileUploadService.UploadResult> results = fileUploadService.uploadBatch(files, category);
        return Result.success("批量上传成功", results);
    }

    @Operation(summary = "删除文件", description = "根据相对 URL 删除已上传的文件")
    @DeleteMapping("/delete")
    public Result<Map<String, Object>> delete(
            @Parameter(description = "文件相对 URL，如 /uploads/material/2026/09/xxx.jpg")
            @RequestParam String url) {

        boolean ok = fileUploadService.delete(url);
        Map<String, Object> data = new HashMap<>();
        data.put("url", url);
        data.put("deleted", ok);
        return Result.success(data);
    }
}
