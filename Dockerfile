# syntax=docker/dockerfile:1

# --- Stage 1: build -------------------------------------------------------
# Compila o jar dentro do próprio Docker para que "docker-compose up -d"
# não dependa de o desenvolvedor ter rodado o build na IDE antes.
FROM eclipse-temurin:21-jdk-jammy AS build
WORKDIR /workspace

COPY gradlew gradlew.bat build.gradle.kts settings.gradle.kts ./
COPY gradle ./gradle

# Baixa dependências e o distributable do Gradle numa layer separada — evita
# rebaixar tudo a cada mudança de código-fonte (só invalida se o build.gradle
# mudar).
RUN chmod +x gradlew && ./gradlew --no-daemon dependencies || true

COPY src ./src

RUN ./gradlew --no-daemon clean bootJar

# --- Stage 2: runtime -------------------------------------------------------
FROM eclipse-temurin:21-jre-jammy AS runtime
WORKDIR /app

# curl é exigido pelo HEALTHCHECK abaixo, usado pelo docker-compose
# (depends_on: condition: service_healthy) para só liberar dependentes da
# app depois que o /actuator/health responder 200.
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*

COPY --from=build /workspace/build/libs/*.jar app.jar

EXPOSE 8080

HEALTHCHECK --interval=10s --timeout=5s --start-period=40s --retries=5 \
    CMD curl -f http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]
