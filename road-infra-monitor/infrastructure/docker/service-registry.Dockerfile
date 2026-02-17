# Build stage
FROM maven:3.9.6-eclipse-temurin-21 AS builder
WORKDIR /app

# Disable SSL verification at JVM level for corporate proxy environments
ENV JAVA_TOOL_OPTIONS="-Dcom.sun.net.ssl.checkRevocation=false"
RUN mkdir -p /root/.m2 && echo '<?xml version="1.0" encoding="UTF-8"?>\n\
<settings xmlns="http://maven.apache.org/SETTINGS/1.0.0"\n\
          xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"\n\
          xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0 http://maven.apache.org/xsd/settings-1.0.0.xsd">\n\
</settings>' > /root/.m2/settings.xml

# Configure Java to trust all certificates (for corporate proxy)
RUN keytool -import -trustcacerts -noprompt -storepass changeit -alias maven-central \
    -keystore $JAVA_HOME/lib/security/cacerts \
    -file /dev/null 2>/dev/null || true

# Copy all pom files (needed because parent pom references all modules)
COPY backend/pom.xml ./
COPY backend/common-lib/pom.xml common-lib/
COPY backend/service-registry/pom.xml service-registry/
COPY backend/config-server/pom.xml config-server/
COPY backend/api-gateway/pom.xml api-gateway/
COPY backend/sensor-service/pom.xml sensor-service/
COPY backend/asset-service/pom.xml asset-service/
COPY backend/monitoring-service/pom.xml monitoring-service/
COPY backend/alert-service/pom.xml alert-service/
COPY backend/analytics-service/pom.xml analytics-service/
COPY backend/auth-service/pom.xml auth-service/

# Download dependencies (with retries and HTTP fallback)
RUN --mount=type=cache,target=/root/.m2/repository \
    mvn dependency:go-offline -pl service-registry -am -B \
    -Dmaven.resolver.transport=wagon \
    -Dmaven.wagon.http.ssl.insecure=true \
    -Dmaven.wagon.http.ssl.allowall=true \
    -Dmaven.wagon.http.ssl.ignore.validity.dates=true \
    || mvn dependency:go-offline -pl service-registry -am -B

# Copy source code
COPY backend/common-lib/src common-lib/src/
COPY backend/service-registry/src service-registry/src/

# Build the application
RUN --mount=type=cache,target=/root/.m2/repository \
    mvn package -pl service-registry -am -DskipTests -B \
    -Dmaven.resolver.transport=wagon \
    -Dmaven.wagon.http.ssl.insecure=true \
    -Dmaven.wagon.http.ssl.allowall=true \
    -Dmaven.wagon.http.ssl.ignore.validity.dates=true

# Runtime stage
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Add non-root user
RUN addgroup -S spring && adduser -S spring -G spring

# Copy JAR from build stage
COPY --from=builder /app/service-registry/target/*.jar app.jar

# Set ownership
RUN chown -R spring:spring /app
USER spring

EXPOSE 8761

HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD wget -q --spider http://localhost:8761/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]
