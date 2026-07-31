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
COPY account-service/account-service/pom.xml ./account-service/

# -----------------------------------------------------
# Build the account-service application
# -----------------------------------------------------
WORKDIR /build/account-service

RUN mvn -B dependency:go-offline

COPY account-service/account-service/src ./src

RUN mvn -B clean package -DskipTests

# =====================================================
# Stage 2: Create the runtime image
# Purpose: Build a lightweight image containing only
#          the packaged Spring Boot application
# =====================================================
FROM eclipse-temurin:21-jre-jammy

WORKDIR /app

COPY --from=builder /build/account-service/target/account-service-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8082

ENTRYPOINT ["java", "-jar", "app.jar"]