FROM maven:3.9.6-eclipse-temurin-17-alpine

WORKDIR /app

# Create a non-root user and group
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

# Copy pom.xml first for better dependency caching
COPY pom.xml .



# Download dependencies
RUN mvn dependency:go-offline -B

# Copy source code
COPY src src

# Build the application
RUN mvn clean package -DskipTests -B

# Give the non-root user ownership of the application directory
RUN chown -R appuser:appgroup /app

# Run the application as a non-root user
USER appuser

# Run the application with proper JVM settings for Cloud Run
EXPOSE 8080
CMD ["sh", "-c", "java -server -Xmx512m -XX:+UseG1GC -XX:MaxGCPauseMillis=100 -Dspring.jmx.enabled=false -Djava.security.egd=file:/dev/./urandom -Dserver.address=0.0.0.0 -Dserver.port=${PORT:-8080} -jar target/*.jar"]
