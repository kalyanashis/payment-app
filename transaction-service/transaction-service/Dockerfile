# =====================================================
# Stage 1: Build project artifacts
# Purpose: Build the shared library and package the
#          Spring Boot application
# =====================================================
FROM maven:3.9.11-eclipse-temurin-21 AS builder

WORKDIR /build

# -----------------------------------------------------
# Copy Maven POM files first to maximize Docker layer
# caching for dependency resolution
# -----------------------------------------------------
COPY common-security/pom.xml ./common-security/
COPY transaction-service/transaction-service/pom.xml ./transaction-service/

# -----------------------------------------------------
# Build and install the shared common-security library
# -----------------------------------------------------
WORKDIR /build/common-security

RUN mvn -B dependency:go-offline

COPY common-security/src ./src

RUN mvn -B clean install -DskipTests

# -----------------------------------------------------
# Build the transaction-service application
# -----------------------------------------------------
WORKDIR /build/transaction-service

RUN mvn -B dependency:go-offline

COPY transaction-service/transaction-service/src ./src

RUN mvn -B clean package -DskipTests

# =====================================================
# Stage 2: Create the runtime image
# Purpose: Build a lightweight image containing only
#          the packaged Spring Boot application
# =====================================================
FROM eclipse-temurin:21-jre-jammy

WORKDIR /app

COPY --from=builder /build/transaction-service/target/transaction-service-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8083

ENTRYPOINT ["java", "-jar", "app.jar"]