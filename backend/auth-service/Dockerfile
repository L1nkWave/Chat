FROM eclipse-temurin:18-jre-alpine
WORKDIR /app
COPY --from=linkwave/backend:latest ./app/auth-service/target/auth-service-0.0.1-SNAPSHOT.jar ./auth-service.jar
CMD ["java", "-jar", "auth-service.jar"]