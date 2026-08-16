# Stage 1: Build
FROM maven:3.9.9-eclipse-temurin-17-alpine AS builder
# FROM maven:3.9.9-eclipse-temurin-17 AS builder
WORKDIR /app

# Step 1: Cache dependencies (Huge time saver)
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Step 2: Build the application
COPY src ./src
# For Local Development
RUN mvn clean package -DskipTests
# For CI/CD Pipeline (GitHub Actions, Jenkins) & Docker
# RUN mvn clean package -DskipTests -B

# Stage 2: Runtime
# Use 'alpine' version to keep the image size small (~100MB vs ~300MB)
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Step 3: Security - Add non-root user
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

# Step 4: Copy the artifact from the builder stage
COPY --from=builder /app/target/*.jar app.jar

# Step 5: Healthcheck (For the port 8084)
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD wget -q --spider http://localhost:8084/actuator/health || exit 1
  
EXPOSE 8084

# Step 6: JVM Memory Tuning for Containers
ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]