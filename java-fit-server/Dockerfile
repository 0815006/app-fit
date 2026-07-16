# =============================================
# 本地 docker-compose 构建用 Dockerfile
# 上下文：java-fit-server/
# =============================================
FROM maven:3.9-eclipse-temurin-21-alpine AS builder
WORKDIR /build
COPY pom.xml .
# 预先下载依赖（利用 Docker 层缓存）
RUN mvn dependency:go-offline -B -q || true
COPY src ./src
RUN mvn clean package -Dmaven.test.skip=true -q

FROM eclipse-temurin:21-jre-alpine
ENV TZ=Asia/Shanghai
WORKDIR /app
COPY --from=builder /build/target/*.jar app.jar
EXPOSE 8091
HEALTHCHECK --interval=30s --timeout=3s --retries=3 CMD wget -q --spider http://localhost:8091/actuator/health || exit 1
ENTRYPOINT ["java", "-Duser.timezone=Asia/Shanghai", "-jar", "app.jar"]