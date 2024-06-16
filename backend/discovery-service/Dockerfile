FROM eclipse-temurin:18-jre-alpine
WORKDIR /app
COPY --from=linkwave/backend:latest ./app/discovery-service/target/discovery-service-0.0.1-SNAPSHOT.jar ./discovery-service.jar
CMD ["java", "-jar", "discovery-service.jar"]