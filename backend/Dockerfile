FROM maven:3.8.6-openjdk-18 AS build
WORKDIR /app
COPY . .
RUN mvn clean package spring-boot:repackage -DskipTests
