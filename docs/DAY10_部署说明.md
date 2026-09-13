# Day 10 部署说明（docker-compose 全栈编排）

> 编排负责人：Python 侧（lingmou_ai）。Java 代码零改动，前端代码零改动，全部通过环境变量 / nginx 配置适配。
> 本机无 Docker 时只做静态校验，`docker compose up` 验证需在装有 Docker Desktop 的机器执行。

## 一、新增文件清单

| 文件 | 归属 | 说明 |
|------|------|------|
| `docker-compose.yml` | 编排（Python 侧牵头） | 全栈 5 服务：mysql / redis / ai / backend / frontend |
| `.env.example`（根目录） | 编排 | 根环境变量模板（DB_PASSWORD / JWT_SECRET） |
| `lingmou_ai/Dockerfile` | Python | 已有；0.0.0.0:8000，自带 /api/ai/health 健康检查 |
| `lingmou_backend/Dockerfile` | Java-A 复核 | 多阶段构建：Maven 打包 → temurin-21-jre，curl 健康检查 `/api/test` |
| `lingmou_backend/.dockerignore` | Java-A 复核 | 排除 target/ uploads/ .git |
| `gonghang_vue3/Dockerfile` | 前端 gao 复核 | 多阶段构建：npm build → nginx:1.27-alpine |
| `gonghang_vue3/nginx.conf` | 前端 gao 复核 | 代理规则与 vite.config.ts 逐条对齐 |

## 二、关键设计（为什么 Java/前端代码不用改）

1. **Java 连 localhost 改容器名**：Spring Boot 松散绑定，compose 注入 `SPRING_DATASOURCE_URL`、`SPRING_DATA_REDIS_HOST` 即可覆盖 application.yml，`DB_PASSWORD`/`JWT_SECRET`/`AI_BASE_URL` 本就有 `${ENV:default}` 占位。
2. **前端代理改 nginx**：nginx.conf 按 vite.config.ts 复刻——`/api/ai/` → ai:8000（含 WebSocket Upgrade，数字人 `/api/ai/chat/ws` 可用），其余 `/api`、`/uploads`、`/v3`、`/swagger-ui.html` → backend:8080，前端 history 路由 `try_files` 兜底。
3. **密钥不入 Git**：根 `.env`（已加 .gitignore）放 DB_PASSWORD/JWT_SECRET；`lingmou_ai/.env` 放 DASHSCOPE_API_KEY。缺 key 时 AI 自动规则降级，不影响启动。
4. **依赖顺序**：mysql/redis 健康检查通过后才起 backend；redis 健康后才起 ai；避免 Spring 启动期连库失败。

## 三、启动步骤（有 Docker 的机器）

```bash
# 0. 前置：Docker Desktop 4.25+（Compose v2.24+，ai 服务 env_file 需要 required: false）

# 1. 准备环境变量（两份）
cp .env.example .env                    # 根目录，改 DB_PASSWORD/JWT_SECRET
cp lingmou_ai/.env.example lingmou_ai/.env   # 填 DASHSCOPE_API_KEY（可选，缺省走规则降级）

# 2. 一键构建 + 启动
docker compose up -d --build

# 3. 看健康状态（全部 healthy/running 即就绪，backend 首次启动约 30-60s）
docker compose ps

# 4. 初始化数据库表（首次部署）
#    MySQL 容器已自动建库 lingmou（utf8mb4），表结构需手动导入 schema.sql
#    （MyBatis-Plus 不自动建表）：
docker compose exec -T mysql mysql -uroot -p"$(grep DB_PASSWORD .env | cut -d= -f2)" lingmou < lingmou_backend/src/main/resources/db/schema.sql
```

## 四、验收 checklist

| # | 验证项 | 命令/操作 | 预期 |
|---|--------|-----------|------|
| 1 | 容器状态 | `docker compose ps` | 5 个服务 Up，mysql/redis/ai/backend healthy |
| 2 | 后端探活 | 浏览器 `http://localhost/api/test` | `{"code":0,...}` |
| 3 | AI 直连 | `http://localhost:8000/api/ai/health` | code=0 |
| 4 | AI 经 nginx | 前端页发起数字人对话 | 正常回复（规则降级版） |
| 5 | 前端入口 | `http://localhost` | 登录页可登录（13800138000/123456） |
| 6 | T3 主链路 | 预填单 → 直通码 → 预约 | 材料预检 + T 码签发正常 |
| 7 | 热力图 | 首页热力图卡片 | 6 网点 × 8 时段 |
| 8 | WebSocket | 数字人页发消息 | 收到回复，无断连 |

## 五、常见问题

| 现象 | 原因 | 处理 |
|------|------|------|
| ai 起不来报 env_file | Docker Compose < v2.24 不支持 `required: false` | 升级 Docker Desktop，或手动 `cp lingmou_ai/.env.example lingmou_ai/.env` |
| backend 反复重启 | MySQL 未就绪/密码不一致 | `docker compose logs backend`；确认根 `.env` 的 DB_PASSWORD 与首次建库时一致 |
| 80/8080/8000 端口被占 | 本机跑着原生服务 | 停掉本机 mysql/redis/java/uvicorn，或改 compose 端口映射 |
| 二维码图片 404 | uploads 卷无历史文件 | 容器化后是全新 uploads 卷，重新上传即可 |
| AI 对话走降级 | DASHSCOPE_API_KEY 未配或过期 | 更新 lingmou_ai/.env 后 `docker compose up -d ai`（演示可接受降级） |

## 六、与 Day10 E2E 报告的关系

docs/DAY10_E2E报告.md（Java-A）为主链路功能验收（本机原生跑通 16 步）；本文档为容器化部署验收，两者互补。容器化后建议复跑 E2E 报告第一节的 16 步走查作为最终验收。
