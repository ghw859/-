# ICBC · 灵枢 Java 后端（Day 10，compose 编排配套）
# 构建：docker build -t lingmou-backend .
# 多阶段构建：阶段1 Maven 打包，阶段2 仅保留 JRE + jar，镜像更小
FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /app

# 先拷 pom 单独下依赖，利用 Docker 层缓存（src 变动时不用重新拉依赖）
COPY pom.xml .
RUN mvn -B dependency:go-offline

COPY src ./src
RUN mvn -B package -DskipTests

# ---- 运行阶段 ----
FROM eclipse-temurin:21-jre-jammy

# slim 系镜像无 curl，装一个给 HEALTHCHECK 用
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*

ENV TZ=Asia/Shanghai

WORKDIR /app
COPY --from=build /app/target/lingmou-1.0.0.jar app.jar

# 上传文件目录（compose 挂 volume 持久化）
RUN mkdir -p /app/uploads

EXPOSE 8080

# 健康检查：/api/test 为公开探活端点（README 约定）
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=5 \
    CMD curl -sf http://127.0.0.1:8080/api/test || exit 1

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
