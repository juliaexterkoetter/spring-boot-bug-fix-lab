# syntax=docker/dockerfile:1
FROM maven:3.9.11-eclipse-temurin-21@sha256:6fdc855a6ed81d288ca7ca37ac6ff5e9308b612485c0801d70b25a858c83d237 AS build
WORKDIR /build

COPY pom.xml ./
COPY src ./src

# Optional secrets support restricted build environments without storing proxy settings or CA trust in layers.
RUN --mount=type=cache,target=/root/.m2 \
    --mount=type=secret,id=maven_settings \
    --mount=type=secret,id=java_cacerts \
    set -eu; \
    if [ -f /run/secrets/java_cacerts ]; then \
        export MAVEN_OPTS="${MAVEN_OPTS:-} -Djavax.net.ssl.trustStore=/run/secrets/java_cacerts"; \
    fi; \
    set --; \
    if [ -f /run/secrets/maven_settings ]; then \
        set -- --settings /run/secrets/maven_settings; \
    fi; \
    mvn --batch-mode "$@" clean verify

FROM eclipse-temurin:21-jre-alpine@sha256:51ab5e3302e7141ce665ca3ea85e8b5cd648eafbc3c0c90dd79d6537684e4555 AS runtime
WORKDIR /app
COPY --from=build --chown=10001:10001 /build/target/spring-boot-bug-fix-lab-0.0.1-SNAPSHOT.jar /app/application.jar
USER 10001:10001
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/application.jar"]
