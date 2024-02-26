FROM eclipse-temurin:18-jre-alpine
WORKDIR /app
COPY ./target/ws-0.0.1-SNAPSHOT.jar ./ws-server.jar
CMD ["java", "-jar", "ws-server.jar"]