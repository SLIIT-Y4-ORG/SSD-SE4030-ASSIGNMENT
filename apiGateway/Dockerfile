# Build stage
FROM maven:3.9.6-eclipse-temurin-17-alpine AS build

WORKDIR /app

# Copy pom.xml first to improve dependency caching
COPY pom.xml .

# Download dependencies
RUN mvn dependency:go-offline -B

# Copy source code
COPY src ./src

# Build the application
RUN mvn clean package -DskipTests -B


# Runtime stage
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Copy the built application from the build stage
COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080

CMD ["sh", "-c", "java -server -Xmx512m -XX:+UseG1GC -XX:MaxGCPauseMillis=100 -Dspring.jmx.enabled=false -Djava.security.egd=file:/dev/./urandom -Dserver.address=0.0.0.0 -Dserver.port=${PORT:-8080} -jar app.jar"]