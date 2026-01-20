# 빌드 스테이지
FROM gradle:7-jdk17 AS builder
WORKDIR /app
COPY . .
RUN gradle clean build -x test

# 실행 스테이지
FROM eclipse-temurin:17-jdk-jammy
COPY --from=builder /app/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-Xms256", "-Xmx512", "-XX:NativeMemoryTracking=summary", "-Xlog:gc*:file=/var/log/slam-api/gc_%t.log:time,uptime,level,tags:filecount=5,filesize=10m", "-Dspring.profiles.active=prod", "-jar", "/app.jar"]