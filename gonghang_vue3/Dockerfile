# ICBC · 灵枢前端镜像（nginx 托管 Vue3 构建产物）
#
# 构建方式：产物预构建 + nginx 托管
#   1) 在宿主机（或 CI）执行：npm ci --legacy-peer-deps && npm run build
#   2) docker build -t lingmou-frontend:1.0.0 .
#
# 说明：原多阶段方案需要 node:24-alpine 基础镜像。当前环境镜像仓库
# 无法稳定拉取 node 系列镜像，故改为消费已构建的 dist/ 产物，
# 运行镜像与多阶段方案完全一致（nginx:1.27-alpine），不影响运行行为。
# 若后续可正常拉取 node 镜像，可恢复多阶段构建。

FROM nginx:1.27-alpine

ENV TZ=Asia/Shanghai

# 反向代理配置：/api/ai → Python 8000（含 WebSocket），其余 /api → Java 8080
COPY nginx.conf /etc/nginx/conf.d/default.conf

# Vue3 构建产物
COPY dist /usr/share/nginx/html

EXPOSE 80

HEALTHCHECK --interval=30s --timeout=5s --start-period=10s --retries=3 \
    CMD wget -q --spider http://127.0.0.1/ || exit 1

CMD ["nginx", "-g", "daemon off;"]
