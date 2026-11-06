# ==============================================================================
# Stage 1: Build Stage
# ==============================================================================
FROM maven:3.9-eclipse-temurin-21-alpine AS builder

WORKDIR /build

# Leverage Docker layer caching for Maven dependencies
COPY pom.xml .
RUN mvn dependency:go-offline -B || true

# Copy source code and build production artifact
COPY src ./src
RUN mvn clean package -DskipTests -B

# ==============================================================================
# Stage 2: Production Runtime Stage
# ==============================================================================
FROM eclipse-temurin:21-jre-alpine AS runner

# Security: Create dedicated non-root application user and group
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

WORKDIR /app

# Copy built JAR artifact from builder stage
COPY --from=builder /build/target/*.jar app.jar

# Enforce secure ownership and permissions
RUN chown -R appuser:appgroup /app

# Run as non-root user for container security hardening
USER appuser

# Expose HTTP port
EXPOSE 8080

# Container-aware JVM memory settings
ENV JAVA_TOOL_OPTIONS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"

# Application entrypoint
ENTRYPOINT ["java", "-jar", "app.jar"]

