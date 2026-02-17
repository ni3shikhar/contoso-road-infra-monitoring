# Build stage
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app

COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
COPY common-lib/pom.xml common-lib/
COPY api-gateway/pom.xml api-gateway/

RUN ./mvnw dependency:go-offline -pl api-gateway -am

COPY common-lib/src common-lib/src/
COPY api-gateway/src api-gateway/src/

RUN ./mvnw package -pl api-gateway -am -DskipTests

# Runtime stage
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN addgroup -S spring && adduser -S spring -G spring

COPY --from=builder /app/api-gateway/target/*.jar app.jar

RUN chown -R spring:spring /app
USER spring

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
