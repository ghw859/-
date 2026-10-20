# ICBC · 灵枢 Java 后端镜像
#
# 构建方式：产物预构建 + JRE 运行
#   1) 在宿主机（或 CI）执行：mvn clean package -DskipTests
#   2) docker build -t lingmou-backend:1.0.0 .
#
# 说明：原多阶段方案需要 maven:3.9-eclipse-temurin-21 基础镜像。当前环境
# 镜像仓库对大体积镜像拉取不稳定，故改为消费已构建的 jar，运行镜像与
# 多阶段方案一致（eclipse-temurin:21-jre-alpine），运行行为不变。
# 若后续可稳定拉取 maven 镜像，可恢复多阶段构建以获得完全可复现的构建。

FROM eclipse-temurin:21-jre-alpine

ENV TZ=Asia/Shanghai \
    JAVA_OPTS="-Xms256m -Xmx512m -Duser.timezone=Asia/Shanghai"

WORKDIR /app

# 上传文件目录（compose 挂 volume 持久化）
RUN mkdir -p /app/uploads

COPY target/lingmou-1.0.0.jar app.jar

EXPOSE 8080

# 健康检查：/api/test 为公开探活端点；alpine 自带 busybox wget，无需额外安装
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=5 \
    CMD wget -q --spider http://127.0.0.1:8080/api/test || exit 1

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
