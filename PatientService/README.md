# Patient Service

Patient profile management microservice for the Clinic Management System.

## Overview

This microservice handles:
- **Patient Profile Management** — Create, read, update patient profiles linked to user accounts

> Appointment management is handled by a dedicated **AppointmentService**.

## Integration Points

| Service | Direction | Purpose |
|---------|-----------|---------|
| **user-service** | Patient → User | Token validation via `GET /api/auth/validate` |

## API Endpoints

### Patient Endpoints (`/api/patients`)

| Method | Path | Auth | Role | Description |
|--------|------|------|------|-------------|
| POST | `/api/patients` | Required | Any | Create patient profile |
| GET | `/api/patients` | Required | ADMIN, RECEPTIONIST | List all patients |
| GET | `/api/patients/me` | Required | Any | Get own patient profile |
| GET | `/api/patients/{id}` | Required | Any | Get patient by ID |
| PUT | `/api/patients/{id}` | Required | Self/ADMIN/RECEPTIONIST | Update patient profile |

### Health

| Method | Path | Description |
|--------|------|-------------|
| GET | `/actuator/health` | Spring Boot health check |
| GET | `/health` | Custom health endpoint |

## Tech Stack

- Java 17, Spring Boot 3.2.3
- Spring Data JPA + PostgreSQL (Supabase, `patient` schema)
- Docker (multi-stage build)
- Spring Boot Actuator for health checks

## Environment Variables

| Variable | Description |
|----------|-------------|
| `PORT` | Server port (default: `8080`) |
| `DB_URL` | PostgreSQL JDBC URL (Supabase Session Pooler) |
| `DB_USERNAME` | Database username (`postgres.<project-ref>`) |
| `DB_PASSWORD` | Database password (**required**) |
| `USER_SERVICE_URL` | User service base URL for token validation |

## Running Locally

```powershell
# PowerShell — set env vars and start
$env:DB_URL="jdbc:postgresql://aws-1-ap-south-1.pooler.supabase.com:5432/postgres?prepareThreshold=0&sslmode=require"
$env:DB_USERNAME="postgres.<project-ref>"
$env:DB_PASSWORD="your_db_password"
$env:USER_SERVICE_URL="https://user-service-268672367192.us-central1.run.app"
java -jar target/patient-service-1.0.0.jar
```

```bash
# Linux/macOS
export DB_URL="jdbc:postgresql://aws-1-ap-south-1.pooler.supabase.com:5432/postgres?prepareThreshold=0&sslmode=require"
export DB_USERNAME="postgres.<project-ref>"
export DB_PASSWORD="your_db_password"
export USER_SERVICE_URL="https://user-service-268672367192.us-central1.run.app"
./mvnw clean package -DskipTests
java -jar target/patient-service-1.0.0.jar
```

## Docker

```bash
docker build -t patient-service .
docker run -p 8080:8080 \
  -e DB_URL="jdbc:postgresql://..." \
  -e DB_USERNAME="postgres.<project-ref>" \
  -e DB_PASSWORD="your_db_password" \
  -e USER_SERVICE_URL="https://user-service-268672367192.us-central1.run.app" \
  patient-service
```

## CI/CD Pipeline

This service uses **GitHub Actions** (`.github/workflows/ci-cd.yml`) with three stages:

| Stage | Trigger | Action |
|-------|---------|--------|
| **Build & Test** | Every push / PR to `main` | `mvn clean verify` |
| **Docker Build & Push** | Push to `main` only | Builds image, pushes to GCP Artifact Registry |
| **Deploy** | Push to `main` only | Deploys to Google Cloud Run |

### Required GitHub Secrets

Go to **GitHub → Repository → Settings → Secrets and variables → Actions** and add:

| Secret | Description |
|--------|-------------|
| `GCP_PROJECT_ID` | Your Google Cloud project ID |
| `GCP_SA_KEY` | GCP Service Account key JSON (with Artifact Registry + Cloud Run permissions) |

The `DB_PASSWORD` is sourced from **GCP Secret Manager** (secret name: `patient-service-db-password`).  
Create it once with:
```bash
echo -n "your_db_password" | gcloud secrets create patient-service-db-password --data-file=-
```
Then grant the Cloud Run service account access:
```bash
gcloud secrets add-iam-policy-binding patient-service-db-password \
  --member="serviceAccount:<SA_EMAIL>" \
  --role="roles/secretmanager.secretAccessor"
```

### Manual deploy via Cloud Build

```bash
gcloud builds submit --config cloudbuild.yaml
```

## Deployment (Choreo)

Alternatively, deploy on Choreo using the included `Dockerfile`:
- Multi-stage build: Maven → `eclipse-temurin:17-jre-alpine`
- Non-root user for security
- Set environment variables in **Choreo Console → Component → DevOps → Configs & Secrets**
