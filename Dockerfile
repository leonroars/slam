# 빌드 스테이지
FROM gradle:7-jdk17 AS builder
WORKDIR /app
COPY . .
RUN gradle clean build -x test

# 실행 스테이지
FROM eclipse-temurin:17-jre-jammy
COPY --from=builder /app/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-Xms256m", "-Xmx512m", "-Dspring.profiles.active=prod", "-jar", "/app.jar"]