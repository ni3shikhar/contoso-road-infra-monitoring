# Build stage
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app

COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
COPY common-lib/pom.xml common-lib/
COPY auth-service/pom.xml auth-service/

RUN ./mvnw dependency:go-offline -pl auth-service -am

COPY common-lib/src common-lib/src/
COPY auth-service/src auth-service/src/

RUN ./mvnw package -pl auth-service -am -DskipTests

# Runtime stage
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN addgroup -S spring && adduser -S spring -G spring

COPY --from=builder /app/auth-service/target/*.jar app.jar

RUN chown -R spring:spring /app
USER spring

EXPOSE 8086

ENTRYPOINT ["java", "-jar", "app.jar"]
