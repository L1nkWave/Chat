FROM maven:3.9.6-eclipse-temurin-21-alpine AS build
WORKDIR /app
COPY ./pom.xml .
COPY ./src ./src
RUN mvn clean package -DskipTests

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/target/user-service-0.0.1-SNAPSHOT.jar ./user-service.jar
CMD ["java", "-jar", "user-service.jar"]