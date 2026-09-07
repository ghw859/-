package com.icbc.lingmou;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * ICBC · 灵枢 后端服务启动类
 */
@SpringBootApplication
@EnableScheduling // 开启定时任务（过期失效、客流更新等）
@MapperScan("com.icbc.lingmou.mapper")
public class LingmouApplication {

    public static void main(String[] args) {
        SpringApplication.run(LingmouApplication.class, args);
        System.out.println("""

                ╔══════════════════════════════════════════╗
                ║   ICBC · 灵枢后端服务启动成功!              ║
                ║   接口地址: http://localhost:8080         ║
                ║   接口文档: http://localhost:8080/swagger ║
                ╚══════════════════════════════════════════╝
                """);
    }
}
