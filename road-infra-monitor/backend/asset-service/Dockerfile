# Build stage
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app

COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
COPY common-lib/pom.xml common-lib/
COPY asset-service/pom.xml asset-service/

RUN ./mvnw dependency:go-offline -pl asset-service -am

COPY common-lib/src common-lib/src/
COPY asset-service/src asset-service/src/

RUN ./mvnw package -pl asset-service -am -DskipTests

# Runtime stage
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN addgroup -S spring && adduser -S spring -G spring

COPY --from=builder /app/asset-service/target/*.jar app.jar

RUN chown -R spring:spring /app
USER spring

EXPOSE 8082

ENTRYPOINT ["java", "-jar", "app.jar"]
