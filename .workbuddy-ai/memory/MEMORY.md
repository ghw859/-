# 灵枢项目长期记忆

## 项目概况
- 项目名：ICBC·灵枢（工行杯参赛项目）
- 类型：银行智慧网点排队预约+AI辅助办理全栈系统
- 架构：前端(Vue3) + 后端(Java Spring Boot) + AI服务(Python FastAPI) 三模块微服务
- 部署：Docker Compose 编排，nginx 统一入口 :80

## 技术栈
- 前端：Vue 3.5 / Vite 8 / TypeScript 6 / Pinia / TailwindCSS 3 / Axios
- 后端：Java 21 / Spring Boot 3.2.5 / MyBatis-Plus 3.5 / MySQL 8 / Redis 7 / JWT(jjwt 0.12) / ZXing 3.5 / Hutool / Spring Security(BCrypt)
- AI：Python / FastAPI / numpy(线性回归) / 通义千问 qwen-turbo(DashScope) / redis.asyncio

## 关键约定
- 响应统一格式：{code:0成功, msg, data}，AI错误码 6xxxx，后端 1xxxx/2xxxx
- 前端代码冻结原则：后端单方面适配前端契约（Day8起）
- AI预填单解析红线：Python只做规则兜底，严禁调大模型
- 演示账号：13800138000/123456(客户张伟)，audit001/123456(审计员李审)

## 参赛状态
- 同时参加两个赛事：工行杯(ICBC·灵枢) 和 鲁信杯(灵枢·NEXUS)
- 已交付参赛材料：2份软件检测报告(49项用例100%通过) + 软著源程序文档 + 软著用户操作手册
- 最新提交：c67a50e(2026-10-19)，代码逻辑自9月13日起未变，10月新增为参赛文档+部署优化
- docker-compose：MySQL首次启动自动执行 schema.sql 建表+种子数据，5容器全部 unless-stopped 重启策略

## 微信小程序（lingmou_miniapp）
- 生成日期：2026-10-19
- 位置：D:\MyProject\gongxing\lingmou_miniapp\
- 技术：微信原生开发 WXML/WXSS/JS，非框架
- 9个页面：login、register、overview、navigation、preform、qrcode、progress、history、ai-assistant
- 5个tabBar入口：总览看板、预约排队、预填单、直通码、AI助手
- API完全对齐前端Vue3：同一baseUrl，同一token(Bearer)，同一响应解包{code:0→data}
- 降级策略同步：heatmap本地算法、AI对话本地知识库(17条规则)、诊断静态文本
- 颜色方案：ICBC蓝(#1a56db/#3b82f6) + 青(#06b6d4)，与前端一致
