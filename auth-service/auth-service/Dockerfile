# =====================================================
# Stage 1: Build project artifacts
# Purpose: Build and package the Spring Boot application
# =====================================================
FROM maven:3.9.11-eclipse-temurin-21 AS builder

WORKDIR /build

# -----------------------------------------------------
# Copy Maven POM files first to maximize Docker layer
# caching for dependency resolution
# -----------------------------------------------------
COPY auth-service/auth-service/pom.xml ./auth-service/

# -----------------------------------------------------
# Build the auth-service application
# -----------------------------------------------------
WORKDIR /build/auth-service

RUN mvn -B dependency:go-offline

COPY auth-service/auth-service/src ./src

RUN mvn -B clean package -DskipTests

# =====================================================
# Stage 2: Create the runtime image
# Purpose: Build a lightweight image containing only
#          the packaged Spring Boot application
# =====================================================
FROM eclipse-temurin:21-jre-jammy

WORKDIR /app

COPY --from=builder /build/auth-service/target/auth-service-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8081

ENTRYPOINT ["java", "-jar", "app.jar"]