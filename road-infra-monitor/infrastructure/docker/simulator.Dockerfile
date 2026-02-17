# Multi-stage build for Road Infrastructure Monitoring Simulator
FROM maven:3.9-eclipse-temurin-21 AS builder

WORKDIR /app

# Copy parent pom and simulator module
COPY simulator/pom.xml ./simulator/pom.xml

# Download dependencies first (for caching)
WORKDIR /app/simulator
RUN mvn dependency:go-offline -B \
    -Dmaven.resolver.transport=wagon \
    -Dmaven.wagon.http.ssl.insecure=true \
    -Dmaven.wagon.http.ssl.allowall=true || true

# Copy source code
COPY simulator/src ./src

# Build the application
RUN mvn clean package -DskipTests -B \
    -Dmaven.resolver.transport=wagon \
    -Dmaven.wagon.http.ssl.insecure=true \
    -Dmaven.wagon.http.ssl.allowall=true

# Runtime image
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Add non-root user for security
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

# Copy the built JAR
COPY --from=builder /app/simulator/target/*.jar app.jar

# Health check
HEALTHCHECK --interval=30s --timeout=10s --retries=3 --start-period=60s \
    CMD wget -q --spider http://localhost:9000/actuator/health || exit 1

# Expose port
EXPOSE 9000

# Run the application
ENTRYPOINT ["java", "-Xms256m", "-Xmx512m", "-jar", "app.jar"]
