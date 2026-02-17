# Build stage
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app

COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
COPY common-lib/pom.xml common-lib/
COPY sensor-service/pom.xml sensor-service/

RUN ./mvnw dependency:go-offline -pl sensor-service -am

COPY common-lib/src common-lib/src/
COPY sensor-service/src sensor-service/src/

RUN ./mvnw package -pl sensor-service -am -DskipTests

# Runtime stage
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN addgroup -S spring && adduser -S spring -G spring

COPY --from=builder /app/sensor-service/target/*.jar app.jar

RUN chown -R spring:spring /app
USER spring

EXPOSE 8081

ENTRYPOINT ["java", "-jar", "app.jar"]
