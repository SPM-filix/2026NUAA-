# syntax=docker/dockerfile:1.7
# 多阶段构建：Maven 编译 -> JRE 运行
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build
COPY docker-maven-settings.xml /build/settings.xml
COPY pom.xml .
COPY src ./src
# 使用阿里云镜像 + BuildKit 依赖缓存，重复构建不再重新下载
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -q -s /build/settings.xml -DskipTests package

FROM eclipse-temurin:21-jre

ENV TZ=Asia/Shanghai
WORKDIR /app

RUN useradd --system --uid 1001 app \
    && mkdir -p /app/uploads \
    && chown -R app:app /app

COPY --from=build /build/target/*.jar /app/app.jar

USER app
EXPOSE 8080

ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-Duser.timezone=Asia/Shanghai", "-jar", "/app/app.jar"]
