FROM eclipse-temurin:18-jre-alpine
WORKDIR /app
COPY --from=linkwave/backend:latest ./app/chat-service/target/chat-service-0.0.1-SNAPSHOT.jar ./chat-service.jar
CMD ["java", "-jar", "chat-service.jar"]