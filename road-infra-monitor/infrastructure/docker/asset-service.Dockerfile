# Build stage
FROM maven:3.9.6-eclipse-temurin-21 AS builder
WORKDIR /app

ENV JAVA_TOOL_OPTIONS="-Dcom.sun.net.ssl.checkRevocation=false"
RUN mkdir -p /root/.m2

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

RUN --mount=type=cache,target=/root/.m2/repository \
    mvn dependency:go-offline -pl asset-service -am -B \
    -Dmaven.resolver.transport=wagon \
    -Dmaven.wagon.http.ssl.insecure=true \
    -Dmaven.wagon.http.ssl.allowall=true

COPY backend/common-lib/src common-lib/src/
COPY backend/asset-service/src asset-service/src/

RUN --mount=type=cache,target=/root/.m2/repository \
    mvn package -pl asset-service -am -DskipTests -B \
    -Dmaven.resolver.transport=wagon \
    -Dmaven.wagon.http.ssl.insecure=true \
    -Dmaven.wagon.http.ssl.allowall=true

# Runtime stage
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN addgroup -S spring && adduser -S spring -G spring

COPY --from=builder /app/asset-service/target/*.jar app.jar

RUN chown -R spring:spring /app
USER spring

EXPOSE 8082

HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD wget -q --spider http://localhost:8082/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]
