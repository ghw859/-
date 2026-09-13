# ICBC · 灵枢前端（Day 10，compose 编排配套）
# 构建：docker build -t lingmou-frontend .
# 多阶段构建：阶段1 Node 打包 dist，阶段2 nginx 托管静态资源并反向代理后端
# node:24：package.json engines 要求 ^22.18.0 || >=24.12.0，node:20 不满足（首次实跑踩坑）
FROM node:24-alpine AS build

WORKDIR /app

# 先拷 package 文件单独装依赖，利用 Docker 层缓存
COPY package.json package-lock.json ./
# --legacy-peer-deps：vite8/plugin-vue6/eslint10/ts6 的 peer 组合超出 npm ci 默认严格校验，
# 与本地 node22.18+npm10.9 实际安装行为对齐；锁文件未动，产物一致
RUN npm ci --registry=https://registry.npmmirror.com --legacy-peer-deps

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
