# ==========================================
# Stage 1: Build application using Maven
# ==========================================
FROM maven:3.9.9-eclipse-temurin-21-alpine AS builder

WORKDIR /build

# Copy project descriptor and source files
COPY pom.xml .
COPY src ./src

# Build production jar skipping test execution (tests run in CI)
RUN mvn clean package -DskipTests --no-transfer-progress

# ==========================================
# Stage 2: Minimal hardened runtime image
# ==========================================
FROM eclipse-temurin:21-jre-alpine

# Security: Create non-root system user and group
RUN addgroup -S -g 1001 appgroup && \
    adduser -S -u 1001 -G appgroup appuser

WORKDIR /app

# Prepare storage directory with correct permissions
RUN mkdir -p /app/data && \
    chown -R appuser:appgroup /app

# Copy artifact from build stage
COPY --from=builder --chown=appuser:appgroup /build/target/gatherly-api-0.1.0-SNAPSHOT.jar /app/app.jar

# Switch to non-root user
USER appuser

ENV PORT=8080 \
    SPRING_PROFILES_ACTIVE=local

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=5s --start-period=30s --retries=3 \
  CMD wget -qO- http://localhost:${PORT:-8080}/health || exit 1

ENTRYPOINT ["sh", "-c", "exec java -Djava.security.egd=file:/dev/./urandom -jar /app/app.jar"]
