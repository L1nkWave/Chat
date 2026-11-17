# Dockerfile dùng chung cho mọi service. Chọn module bằng build arg MODULE (xem docker-compose.yml).
# Java 21 khớp với maven.compiler.release trong backend/pom.xml.

FROM maven:3.9-eclipse-temurin-21 AS build
ARG MODULE
WORKDIR /app
COPY . .
RUN mvn -B -pl ${MODULE} -am clean package spring-boot:repackage -DskipTests

FROM eclipse-temurin:21-jre
ARG MODULE
# healthcheck trong docker-compose.yml dùng wget
RUN apt-get update \
    && apt-get install -y --no-install-recommends wget \
    && rm -rf /var/lib/apt/lists/*
WORKDIR /app
COPY --from=build /app/${MODULE}/target/*.jar app.jar
ENTRYPOINT ["java", "-jar", "/app/app.jar"]