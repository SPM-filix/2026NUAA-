# 多阶段构建：Maven 编译 -> JRE 运行
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src ./src
RUN mvn -B -q -DskipTests package

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
