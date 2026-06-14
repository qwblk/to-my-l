# syntax=docker/dockerfile:1

# ---------- build ----------
FROM eclipse-temurin:17-jdk-alpine AS build
WORKDIR /workspace

# 先复制 Maven 元数据，利用 Docker layer cache
COPY mvnw pom.xml ./
COPY .mvn .mvn
RUN chmod +x mvnw

# 预下载依赖；失败不终止，避免没有 src 时 go-offline 对某些插件不兼容
RUN ./mvnw -q -DskipTests dependency:go-offline || true

COPY src src
RUN ./mvnw -q -DskipTests package

# ---------- runtime ----------
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

ENV TZ=Asia/Shanghai \
    JAVA_OPTS="" \
    APP_UPLOAD_PATH=/app/uploads

RUN addgroup -S app && adduser -S app -G app \
    && mkdir -p /app/uploads \
    && chown -R app:app /app

COPY --from=build /workspace/target/*.jar /app/app.jar

USER app
EXPOSE 8081

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
