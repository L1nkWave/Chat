FROM eclipse-temurin:18-jre-alpine
WORKDIR /app
COPY --from=linkwave/backend:latest ./app/user-service/target/user-service-0.0.1-SNAPSHOT.jar ./user-service.jar
CMD ["java", "-jar", "user-service.jar"]