# user-service

Spring Boot microservice for user management. Part of a polyrepo microservices setup.

This service is maintained as part of the ClinicMate root repository.

## API

Base URL (local): `http://localhost:8081`

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/api/users` | List all users (admin) |
| `GET` | `/api/users/{id}` | Get own user or any user (admin) |
| `POST` | `/api/users` | Create a user (admin) |
| `PUT` | `/api/users/{id}` | Update own user or any user (admin) |
| `PATCH` | `/api/users/{id}/role` | Assign a role (admin only) |
| `DELETE` | `/api/users/{id}` | Delete own user or any user (admin) |
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
| `SECURITY_TOKEN_SECRET` | none (required) | Random token-signing secret of at least 32 characters |
| `BOOTSTRAP_ADMIN_EMAIL` | empty | Optional initial administrator email |
| `BOOTSTRAP_ADMIN_PASSWORD` | empty | Optional initial administrator password (minimum 12 characters) |
| `BOOTSTRAP_ADMIN_NAME` | `System Administrator` | Initial administrator display name |

## Security behavior

- Public registration always creates an enabled `PATIENT`; identity and privilege fields are assigned server-side.
- New passwords are PBKDF2-HMAC-SHA-256 hashes with 600,000 iterations and unique salts.
- Access tokens expire after one hour; rotating refresh tokens expire after seven days.
- Logout persistently revokes previously issued access and refresh tokens.
- User records are owner-or-admin; user listing, account creation, and role assignment are admin-only.
- Profile updates cannot change roles or account security state, and the final administrator cannot be removed.

## Tech Stack

- Java 17, Spring Boot 3.2
- Spring Data JPA + H2 (in-memory)
- Spring Boot Actuator
- Docker (multi-stage build, non-root user)
- Google Cloud Run
