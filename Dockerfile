#FROM openjdk:17
#ARG JAR_FILE=build/libs/*.jar
#COPY ${JAR_FILE} app.jar
#EXPOSE 8080
#ENTRYPOINT ["java", "-Dspring.profiles.active=prod", "-jar", "/app.jar"]

# 빌드 스테이지
FROM gradle:7-jdk17 AS builder
WORKDIR /app
COPY . .
RUN gradle clean build

# 실행 스테이지
FROM openjdk:17
COPY --from=builder /app/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-Dspring.profiles.active=prod", "-jar", "/app.jar"]