# 빌드 스테이지
FROM gradle:7-jdk17 AS builder
WORKDIR /app
COPY . .
RUN gradle clean build -x test

# 실행 스테이지
FROM eclipse-temurin:17-jre-jammy
COPY --from=builder /app/build/libs/*.jar app.jar
EXPOSE 8080

# EC2 Host 환경 내의 docker-compose 에 정의된 JVM 메모리 옵션을 포함하도록 shell 방식의 실행 옵션 설정으로 변경
ENTRYPOINT ENTRYPOINT exec java $JAVA_OPTS -Dspring.profiles.active=prod -jar /app.jar