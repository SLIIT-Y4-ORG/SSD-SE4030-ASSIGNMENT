# user-service

Spring Boot microservice for user management. Part of a polyrepo microservices setup.

> **Polyrepo**: This service lives in its own Git repository and is independently built, tested, and deployed.

## API

Base URL (local): `http://localhost:8081`

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/api/users` | List all users |
| `GET` | `/api/users/{id}` | Get user by ID |
| `POST` | `/api/users` | Create a user |
| `PUT` | `/api/users/{id}` | Update a user |
| `DELETE` | `/api/users/{id}` | Delete a user |
| `GET` | `/actuator/health` | Health check |

**Create / Update payload:**
```json
{
  "name": "Alice Johnson",
  "email": "alice@example.com",
  "phone": "+1-555-0101",
  "address": "123 Main St"
}
```

## Run Locally

```bash
# Maven
mvn spring-boot:run -Dspring-boot.run.arguments="--server.port=8081"

# Docker
docker build -t user-service .
docker run -p 8081:8080 user-service
```

## Test

```bash
mvn test
```

## Deploy to Google Cloud Run

```bash
# Manual
./deploy.sh YOUR_PROJECT_ID us-central1

# Via Cloud Build (CI/CD)
gcloud builds submit --config=cloudbuild.yaml --project YOUR_PROJECT_ID .
```

## Environment Variables

| Variable | Default | Description |
|---|---|---|
| `PORT` | `8080` | HTTP port (set automatically by Cloud Run) |

## Tech Stack

- Java 17, Spring Boot 3.2
- Spring Data JPA + H2 (in-memory)
- Spring Boot Actuator
- Docker (multi-stage build, non-root user)
- Google Cloud Run
