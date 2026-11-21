# Build/install the complete Maven reactor once, including the shared library.
FROM maven:3.9.9-eclipse-temurin-21 AS build

WORKDIR /workspace
COPY . .

RUN mvn --batch-mode --no-transfer-progress -DskipTests clean install

# Repackage only the selected Spring Boot application. This avoids trying to
# turn the parent POM or the shared library into executable applications.
ARG APP_MODULE
RUN test -n "${APP_MODULE}" \
    && mvn --batch-mode --no-transfer-progress -DskipTests -pl "${APP_MODULE}" package spring-boot:repackage

# Small runtime image; Java 21 is required by backend/pom.xml.
FROM eclipse-temurin:21-jre-jammy AS runtime

RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd --system --gid 10001 app \
    && useradd --system --uid 10001 --gid app --home-dir /app app \
    && mkdir -p /app/storage \
    && chown -R app:app /app

WORKDIR /app

ARG APP_MODULE
ARG APP_JAR
COPY --from=build --chown=app:app /workspace/${APP_MODULE}/target/${APP_JAR} /app/app.jar

USER app:app

ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "/app/app.jar"]
