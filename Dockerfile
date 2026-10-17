# ========================================================
# Multi-Stage Dockerfile for Real-Time Chat Server
# ========================================================

# Stage 1: Build application using Maven and Temurin JDK 17
FROM maven:3.9.6-eclipse-temurin-17 AS builder
WORKDIR /build

# Copy dependency definition and source code
COPY pom.xml .
COPY src ./src
COPY database ./database

# Package shaded executable fat JAR
RUN mvn clean package -DskipTests

# Stage 2: Minimal production runtime with Temurin JRE 17
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

# Copy compiled fat JAR from builder stage
COPY --from=builder /build/target/realtime-chat-app-1.0-SNAPSHOT.jar /app/chat-server.jar

# Expose TLS encrypted chat server socket port
EXPOSE 5000

# Healthcheck to verify TCP port 5000 is accepting connections
HEALTHCHECK --interval=30s --timeout=5s --start-period=10s --retries=3 \
  CMD nc -z localhost 5000 || exit 1

# Launch the server
ENTRYPOINT ["java", "-jar", "/app/chat-server.jar", "server"]
