# --- Stage 1: Build the Application ---
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app

# Copy project files
COPY pom.xml .
COPY src ./src

# Build the JAR file (skipping tests to speed up build)
RUN mvn clean package -DskipTests

# --- Stage 2: Runtime Environment (Playwright Enabled) ---
# We use this specific image because it includes all browsers and dependencies required by Playwright
FROM mcr.microsoft.com/playwright/java:v1.45.0-jammy

LABEL authors="raunak-ranjan"
WORKDIR /app

# Copy the built jar from the previous stage
COPY --from=build /app/target/*.jar app.jar

# Expose the standard Spring Boot port
EXPOSE 8080

# Command to start the application
ENTRYPOINT ["java", "-jar", "app.jar"]