# ICBC · 灵枢前端镜像（多阶段：node 构建 + nginx 托管）

# ---------- 构建阶段 ----------
FROM node:22-alpine AS builder

WORKDIR /app

# 先拷贝依赖清单，最大化利用构建缓存
# eslint-plugin-oxlint 与 oxlint 存在 peer 版本错位，需 --legacy-peer-deps
COPY package.json package-lock.json ./
RUN npm ci --legacy-peer-deps

# 拷贝源码并构建产物（跳过 lint，仅类型检查 + vite build）
COPY . .
RUN npm run build

# ---------- 运行阶段 ----------
FROM nginx:1.27-alpine

ENV TZ=Asia/Shanghai

# 反向代理配置：/api/ai → Python 8000（含 WebSocket），其余 /api → Java 8080
COPY nginx.conf /etc/nginx/conf.d/default.conf

# 构建产物
COPY --from=builder /app/dist /usr/share/nginx/html

EXPOSE 80

HEALTHCHECK --interval=30s --timeout=5s --start-period=10s --retries=3 \
    CMD wget -q --spider http://127.0.0.1/ || exit 1

CMD ["nginx", "-g", "daemon off;"]
