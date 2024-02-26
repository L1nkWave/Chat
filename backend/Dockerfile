FROM maven:3.8.6-openjdk-18 AS build
WORKDIR /app
COPY . .
RUN mvn clean package spring-boot:repackage -DskipTests

# User Service
FROM eclipse-temurin:18-jre-alpine
WORKDIR /app
COPY --from=build /app/user-service/target/user-service-0.0.1-SNAPSHOT.jar ./user-service.jar

# Chat Service
FROM eclipse-temurin:18-jre-alpine
WORKDIR /app
COPY --from=build /app/chat-service/target/chat-service-0.0.1-SNAPSHOT.jar ./chat-service.jar

# Auth Service
FROM eclipse-temurin:18-jre-alpine
WORKDIR /app
COPY --from=build /app/auth-service/target/auth-service-0.0.1-SNAPSHOT.jar ./auth-service.jar

# Discovery Service
FROM eclipse-temurin:18-jre-alpine
WORKDIR /app
COPY --from=build /app/discovery-service/target/discovery-service-0.0.1-SNAPSHOT.jar ./discovery-service.jar

# API Gateway
FROM eclipse-temurin:18-jre-alpine
WORKDIR /app
COPY --from=build /app/api-gateway/target/api-gateway-0.0.1-SNAPSHOT.jar ./api-gateway.jar

# WS Server
FROM eclipse-temurin:18-jre-alpine
WORKDIR /app
COPY --from=build /app/ws-server/target/ws-0.0.1-SNAPSHOT.jar ./ws.jar