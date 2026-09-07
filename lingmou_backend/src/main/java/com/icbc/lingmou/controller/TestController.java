package com.icbc.lingmou.controller;

import com.icbc.lingmou.common.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 测试接口
 * 启动项目后访问 http://localhost:8080/api/test 验证骨架是否正常
 */
@Tag(name = "测试", description = "验证服务是否正常启动")
@RestController
@RequestMapping("/api/test")
public class TestController {

    @Operation(summary = "健康检查", description = "验证服务是否正常启动")
    @GetMapping
    public Result<Map<String, Object>> test() {
        Map<String, Object> data = new HashMap<>();
        data.put("status", "running");
        data.put("service", "ICBC · 灵枢后端服务");
        data.put("time", java.time.LocalDateTime.now().toString());
        return Result.success("服务启动成功", data);
    }
}
