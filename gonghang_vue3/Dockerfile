# ICBC · 灵枢前端（Day 10，compose 编排配套）
# 构建：docker build -t lingmou-frontend .
# 多阶段构建：阶段1 Node 打包 dist，阶段2 nginx 托管静态资源并反向代理后端
FROM node:20-alpine AS build

WORKDIR /app

# 先拷 package 文件单独装依赖，利用 Docker 层缓存
COPY package.json package-lock.json ./
RUN npm ci --registry=https://registry.npmmirror.com

COPY . .
# build = type-check + vite build（Day10 E2E 已验证 exit 0）
RUN npm run build

# ---- 运行阶段 ----
FROM nginx:1.27-alpine

ENV TZ=Asia/Shanghai

# 反向代理配置：/api/ai → Python 8000（含 WebSocket），其余 /api → Java 8080
COPY nginx.conf /etc/nginx/conf.d/default.conf

COPY --from=build /app/dist /usr/share/nginx/html

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
