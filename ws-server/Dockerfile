FROM maven:3.9.6-eclipse-temurin-21-alpine AS build
WORKDIR /app
COPY ./pom.xml .
COPY ./src ./src
RUN mvn clean package -DskipTests

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/target/ws-server-0.0.1-SNAPSHOT.jar ./ws-server.jar
CMD ["java", "-jar", "ws-server.jar"]