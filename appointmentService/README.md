# appointment-service

Appointment management microservice for the clinic system.

Acts as the core service for handling appointment bookings. Currently in active development (integration endpoints for fetching by patient/doctor are pending).

---

## What this service owns

| Resource | Table | Schema |
| :--- | :--- | :--- |
| Appointment records | `appointments` | `appointment` |

<!-- > **Same PostgreSQL instance** as the other services — tables live in the `appointment` schema (isolated from `public` and `doctor`). -->

---

## API Endpoints

Currently, the service supports foundational operations for an appointment.

| Method | Path | Auth | Description |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/appointments` | Any authenticated | Book a new appointment (Triggers Saga) |
| `GET` | `/api/appointments/{id}` | Any authenticated | Get specific appointment details |
| `PATCH` | `/api/appointments/{id}/cancel` | Any authenticated | Cancel an appointment & trigger slot release |

---

## Request / Response Examples

### Book an Appointment

Example HTTP request (include Authorization header when required):

<!-- 
```http
POST /api/appointments
Authorization: Bearer <token>
Content-Type: application/json -->
```http
POST /api/appointments
Content-Type: application/json
```

Request body:

```json
{
  "appointmentDate": "2026-03-15",
  "startTime": "09:00",
  "endTime": "09:30",
  "doctorId": "uuid-of-doctor",
  "patientId": "uuid-of-patient",
  "slotId": "uuid-of-slot",
  "reason": "General Checkup",
  "notes": "Patient requested morning slot",
  "status": "PENDING"
}
```

Response (example):

```json
{
  "id": "uuid-of-appointment",
  "appointmentDate": "2026-03-15",
  "startTime": "09:00",
  "endTime": "09:30",
  "doctorId": "uuid-of-doctor",
  "patientId": "uuid-of-patient",
  "slotId": "uuid-of-slot",
  "reason": "General Checkup",
  "notes": "Patient requested morning slot",
  "status": "CONFIRMED",
  "createdAt": "2026-03-12T12:00:00Z"
}
```

---

## Appointment Lifecycle

```text
PENDING ──payment success──► CONFIRMED
PENDING ──payment failure──► FAILED (Releases Slot)
CONFIRMED ──user cancels───► CANCELLED (Releases Slot)
```

---

## Environment & Configuration

The service runs on port 8084 by default. Base URLs for connecting to other microservices are already configured for future integration.

### Key Configurations

| Property | Value | Description |
| :--- | :--- | :--- |
| `spring.datasource.url` | `jdbc:postgresql://localhost:5432/appointmentdb` | Local PostgreSQL connection |
| `services.user.base-url` | `http://localhost:8081` | User Service URL |
| `services.patient.base-url` | `http://localhost:8082` | Patient Service URL |
| `services.doctor.base-url` | `http://localhost:8083` | Doctor Service URL |

---

## Local Development

### 1. Database Setup

Ensure your local PostgreSQL Docker container is running:

```bash
docker run --name appointment-postgres \
    -e POSTGRES_DB=appointmentdb \
    -e POSTGRES_USER=postgres \
    -e POSTGRES_PASSWORD=postgres \
    -p 5432:5432 \
    -d postgres:16
```

### 2. Run the Service

```bash
# Using Maven wrapper
./mvnw spring-boot:run
```

```bash
# Or using Maven directly:
mvn spring-boot:run
```

The service will start on http://localhost:8084.

### 3. API Documentation

Swagger UI / OpenAPI documentation is built-in. Once the service is running, you can explore and test the available endpoints interactively at:

* **Swagger UI:** [http://localhost:8084/swagger-ui.html](http://localhost:8084/swagger-ui.html) (or `http://localhost:8084/swagger-ui/index.html`)
* **Health Check:** [http://localhost:8084/actuator/health](http://localhost:8084/actuator/health)

---

## Tech Stack
* **Java:** 17
* **Framework:** Spring Boot 4.0.3
* **Database:** PostgreSQL + Spring Data JPA (Hibernate)
* **API Docs:** Springdoc OpenAPI