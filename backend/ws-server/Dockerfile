FROM eclipse-temurin:18-jre-alpine
WORKDIR /app
COPY --from=linkwave/backend:latest ./app/ws-server/target/ws-0.0.1-SNAPSHOT.jar ./ws.jar
CMD ["java", "-jar", "ws.jar"]