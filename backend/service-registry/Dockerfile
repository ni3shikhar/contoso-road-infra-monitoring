# Build stage
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app

# Copy Maven wrapper and pom files
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
COPY common-lib/pom.xml common-lib/
COPY service-registry/pom.xml service-registry/

# Download dependencies
RUN ./mvnw dependency:go-offline -pl service-registry -am

# Copy source code
COPY common-lib/src common-lib/src/
COPY service-registry/src service-registry/src/

# Build the application
RUN ./mvnw package -pl service-registry -am -DskipTests

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

ENTRYPOINT ["java", "-jar", "app.jar"]
