# Build stage
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app

COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
COPY common-lib/pom.xml common-lib/
COPY analytics-service/pom.xml analytics-service/

RUN ./mvnw dependency:go-offline -pl analytics-service -am

COPY common-lib/src common-lib/src/
COPY analytics-service/src analytics-service/src/

RUN ./mvnw package -pl analytics-service -am -DskipTests

# Runtime stage
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN addgroup -S spring && adduser -S spring -G spring

COPY --from=builder /app/analytics-service/target/*.jar app.jar

RUN chown -R spring:spring /app
USER spring

EXPOSE 8085

ENTRYPOINT ["java", "-jar", "app.jar"]
