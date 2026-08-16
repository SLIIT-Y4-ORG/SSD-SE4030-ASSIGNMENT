# doctor-service

Doctor & Schedule microservice for the clinic system.  
Manages doctor profiles and pre-created time slots with reserve/release semantics for saga-based appointment booking.

---

## What this service owns

| Resource       | Table                  | Schema    |
|----------------|------------------------|-----------|
| Doctor profile | `doctor.doctors`       | `doctor`  |
| Time slot      | `doctor.doctor_slots`  | `doctor`  |

> **Same Supabase PostgreSQL instance** as user-service — tables live in the `doctor` schema (isolated from `public`).

---

## API Endpoints

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| `POST` | `/api/doctors` | ADMIN / RECEPTIONIST | Create doctor profile |
| `GET` | `/api/doctors` | Any authenticated | List doctors (filter: `?specialization=` or `?department=`) |
| `GET` | `/api/doctors/{id}` | Any authenticated | Get doctor by ID |
| `POST` | `/api/doctors/{id}/slots` | ADMIN / DOCTOR / RECEPTIONIST | Bulk-create availability slots |
| `GET` | `/api/doctors/{id}/slots?date=YYYY-MM-DD` | Any authenticated | Get all slots for a doctor on a date |
| `GET` | `/api/slots/{slotId}` | Any authenticated | Get slot details |
| `POST` | `/api/slots/{slotId}/reserve` | Any authenticated | Reserve slot (appointment-service → saga step) |
| `POST` | `/api/slots/{slotId}/release` | Any authenticated | Release slot (saga compensation / rollback) |

---

## Request / Response Examples

### Create a doctor
```http
POST /api/doctors
Authorization: Bearer <token>
Content-Type: application/json

{
  "name": "Dr. Ayesha Fernando",
  "specialization": "Cardiology",
  "email": "ayesha@clinic.lk",
  "phone": "+94771234567",
  "licenseNumber": "SLMC-2020-1234",
  "department": "Cardiac Care",
  "yearsOfExperience": 10,
  "userId": "supabase-user-uuid"   // optional link to user-service
}
```

### Create availability slots (bulk)
```http
POST /api/doctors/{id}/slots
Authorization: Bearer <token>
Content-Type: application/json

{
  "slots": [
    { "date": "2026-03-10", "startTime": "09:00", "endTime": "09:30" },
    { "date": "2026-03-10", "startTime": "09:30", "endTime": "10:00" },
    { "date": "2026-03-10", "startTime": "10:00", "endTime": "10:30" }
  ]
}
```

### Reserve a slot (appointment-service calls this)
```http
POST /api/slots/{slotId}/reserve
Authorization: Bearer <token>
Content-Type: application/json

{
  "patientId": "uuid-of-patient",
  "appointmentId": "uuid-of-appointment"
}
```

### Release a slot — saga compensation
```http
POST /api/slots/{slotId}/release
Authorization: Bearer <token>
```

---

## Saga / Integration Story

```
Appointment-Service (Orchestrator)
  │
  ├─► POST /api/slots/{id}/reserve    ← doctor-service
  │         ↓ success
  ├─► POST /api/payments              ← payment-service
  │         ↓ failure (e.g. declined)
  └─► POST /api/slots/{id}/release    ← doctor-service  (compensating tx)
```

The `reserve` / `release` pair makes rolling back a failed booking deterministic.  
`release` is **idempotent** — calling it on an already-available slot is a no-op.

---

## Slot Lifecycle

```
AVAILABLE ──reserve──► RESERVED
RESERVED  ──release──► AVAILABLE
```

Concurrent reservations are handled with a **pessimistic write lock** (`SELECT ... FOR UPDATE`)  
plus a `@Version` optimistic-lock column on the `doctor_slots` table, so two simultaneous  
requests for the same slot result in exactly one success and one HTTP 409.

---

## Environment Variables

| Variable | Required | Default | Description |
|----------|----------|---------|-------------|
| `DB_URL` | Yes | Supabase pooler URL | JDBC connection string |
| `DB_USERNAME` | Yes | — | Supabase DB user |
| `DB_PASSWORD` | Yes | — | Supabase DB password (secret) |
| `USER_SERVICE_URL` | Yes | `http://localhost:8080` | Base URL of user-service for token validation |
| `PORT` | No | `8081` | HTTP port |

---

## Supabase Schema Setup

The `doctor` schema is created automatically on first startup (`schema.sql`).  
If you need to create it manually first (e.g. in Supabase dashboard → SQL editor):

```sql
CREATE SCHEMA IF NOT EXISTS doctor;
```

---

## Local Development

```bash
export DB_PASSWORD=your_supabase_password
export USER_SERVICE_URL=http://localhost:8080   # user-service must be running

mvn spring-boot:run
# Starts on http://localhost:8081

# Health check
curl http://localhost:8081/actuator/health
```

---

## Build & Deploy (Google Cloud Run)

```bash
# First deploy
chmod +x deploy.sh
./deploy.sh <GCP_PROJECT_ID> <USER_SERVICE_CLOUD_RUN_URL>

# Or via Cloud Build (CI/CD trigger)
gcloud builds submit --config cloudbuild.yaml .
```

Required GCP Secret Manager secrets:
- `doctor-service-db-password` — Supabase DB password

---

## Running Tests

```bash
mvn test
```

Tests use Mockito — no database connection required.
