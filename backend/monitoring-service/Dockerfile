# Build stage
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app

COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
COPY common-lib/pom.xml common-lib/
COPY monitoring-service/pom.xml monitoring-service/

RUN ./mvnw dependency:go-offline -pl monitoring-service -am

COPY common-lib/src common-lib/src/
COPY monitoring-service/src monitoring-service/src/

RUN ./mvnw package -pl monitoring-service -am -DskipTests

# Runtime stage
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN addgroup -S spring && adduser -S spring -G spring

COPY --from=builder /app/monitoring-service/target/*.jar app.jar

RUN chown -R spring:spring /app
USER spring

EXPOSE 8083

ENTRYPOINT ["java", "-jar", "app.jar"]
