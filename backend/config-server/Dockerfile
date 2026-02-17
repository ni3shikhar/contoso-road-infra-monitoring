# Build stage
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app

COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
COPY common-lib/pom.xml common-lib/
COPY config-server/pom.xml config-server/

RUN ./mvnw dependency:go-offline -pl config-server -am

COPY common-lib/src common-lib/src/
COPY config-server/src config-server/src/
COPY config-repo/ config-repo/

RUN ./mvnw package -pl config-server -am -DskipTests

# Runtime stage
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN addgroup -S spring && adduser -S spring -G spring

COPY --from=builder /app/config-server/target/*.jar app.jar
COPY --from=builder /app/config-repo/ config-repo/

RUN chown -R spring:spring /app
USER spring

EXPOSE 8888

ENTRYPOINT ["java", "-jar", "app.jar"]
