FROM eclipse-temurin:18-jre-alpine
WORKDIR /app
COPY --from=linkwave/backend:latest ./app/api-gateway/target/api-gateway-0.0.1-SNAPSHOT.jar ./api-gateway.jar
CMD ["java", "-jar", "api-gateway.jar"]