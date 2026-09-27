# Security Findings & Remediation Report (Developer 4)
## SE4030 Secure Software Development Assignment

This document details the security vulnerabilities identified, verified, and remediated by Developer 4.

---

## 1. Vulnerability 1 (C-1): Missing Authentication on Appointment Endpoints

### Vulnerability Summary

| Field | Value |
|---|---|
| **ID** | C-1 |
| **CWE** | CWE-306 — Missing Authentication for Critical Function |
| **OWASP** | A01:2021 – Broken Access Control |
| **Service** | `appointmentService` (`AppointmentController.java`) |
| **Status** | ✅ IMPLEMENTED & VERIFIED |

### Affected Endpoints
Prior to remediation, the following endpoints were completely open and did not enforce authentication:
* `POST /api/appointments`
* `GET /api/appointments/{id}`
* `GET /api/appointments`
* `PATCH /api/appointments/{id}/cancel`
* `POST /api/appointments/{id}/payment-session`
* `POST /api/appointments/payment-callback`
* `GET /api/appointments/{id}/status`

### Root Cause
The `AppointmentController` had zero `@RequestHeader` auth parameters on any endpoint. There was no Spring Security configuration or filter chain. The API Gateway acts as a transparent proxy, meaning requests were passed to the `appointmentService` without any token validation.

### Remediation
1. **Added Token Validation Infrastructure:** Created `UserServiceClient.java` to call `userService` (`GET /api/auth/validate`) and `AuthHelper.java` to serve as a centralized guard checking the `Authorization: Bearer <token>` header.
2. **Endpoint Enforcement:** Added `@RequestHeader(value = "Authorization", required = false)` to all endpoints in `AppointmentController.java`.
3. **Guard Implementation:** Inserted `authHelper.requireBearer(authHeader)` at the start of each endpoint method. By setting `required = false`, Spring Boot reaches the method logic, allowing `AuthHelper` to throw an `UnauthorizedException`, resulting in a clean `HTTP 401 Unauthorized` response rather than a generic `400 Bad Request`.

### Verification Evidence

**BEFORE Evidence (Live curl tests):**
```powershell
# Unauthenticated enumeration of all patient appointments
curl.exe -i http://localhost:8080/api/appointments
# Expected: HTTP 200 OK with full appointment data

# Spoofing a payment callback to bypass Stripe
curl.exe -i -X POST http://localhost:8080/api/appointments/payment-callback `
  -H "Content-Type: application/json" `
  -d '{"appointmentId":"<valid-uuid>","paymentStatus":"success"}'
# Expected: HTTP 200 OK (Payment bypassed)
```

**AFTER Evidence (Live curl tests):**
```powershell
# Unauthenticated enumeration attempt
curl.exe -i http://localhost:8080/api/appointments
# Expected: HTTP 401 Unauthorized {"error":"Unauthorized","message":"Bearer token required"}

# Payment bypass attempt
curl.exe -i -X POST http://localhost:8080/api/appointments/payment-callback `
  -H "Content-Type: application/json" `
  -d '{"appointmentId":"<valid-uuid>","paymentStatus":"success"}'
# Expected: HTTP 401 Unauthorized {"error":"Unauthorized","message":"Bearer token required"}
```

---

## 2. Vulnerability 2 (C-7): Missing Authentication on Payment Profile and Transactions

### Vulnerability Summary

| Field | Value |
|---|---|
| **ID** | C-7 |
| **CWE** | CWE-306 — Missing Authentication for Critical Function |
| **OWASP** | A01:2021 – Broken Access Control |
| **Service** | `paymentService` (`PaymentController.java`) |
| **Status** | ✅ IMPLEMENTED & VERIFIED |

### Affected Endpoints
Prior to remediation, sensitive financial data was exposed on the following endpoints without authentication:
* `GET /api/payments/customers/{userId}` - Exposed Stripe customer ID, email, name
* `GET /api/payments/users/{userId}/transactions` - Exposed full payment history (session IDs, amounts, appointment IDs, status)
* `POST /api/payments/checkout-session`
* `POST /api/payments/checkout-session/{sessionId}/confirm`

*(Note: `POST /api/payments/webhooks/stripe` was intentionally excluded from authentication as it is called by Stripe's servers and is protected by `Stripe-Signature` HMAC-SHA256 verification.)*

### Root Cause
Similar to the `appointmentService`, the `PaymentController` did not enforce any authentication mechanism on user-facing endpoints. Anyone could enumerate Stripe customer IDs or view payment histories by guessing or knowing a user's UUID.

### Remediation
1. **Added Token Validation Infrastructure:** Created `UserServiceClient.java` and `AuthHelper.java` in the `paymentService`.
2. **Environment Configuration:** Configured a `RestTemplate` bean and added the `USER_SERVICE_URL` to `application.yml` and `.env` to allow internal Docker-network communication with the `userService`.
3. **Endpoint Enforcement:** Added `@RequestHeader` and `authHelper.requireBearer(authHeader)` to the 4 public endpoints, ensuring an `HTTP 401 Unauthorized` response when unauthenticated.

### Verification Evidence

**BEFORE Evidence (Live curl tests):**
```powershell
# Unauthenticated exposure of Stripe customer ID
curl.exe -i http://localhost:8080/api/payments/customers/<valid-userId>
# Expected: HTTP 200 OK {"userId":"...","stripeCustomerId":"cus_...","email":"..."}

# Unauthenticated exposure of transaction history
curl.exe -i http://localhost:8080/api/payments/users/<valid-userId>/transactions
# Expected: HTTP 200 OK [{"stripeSessionId":"cs_...","amount":2500.00,"status":"PENDING"}]
```

**AFTER Evidence (Live curl tests):**
```powershell
# Unauthenticated attempts
curl.exe -i http://localhost:8080/api/payments/customers/<valid-userId>
curl.exe -i http://localhost:8080/api/payments/users/<valid-userId>/transactions
# Expected: HTTP 401 Unauthorized {"error":"Unauthorized","message":"Unauthorized"}

# Authenticated attempt
$resp = Invoke-RestMethod -Uri "http://localhost:8080/api/auth/login" -Method Post -ContentType "application/json" -Body '{"email":"doe@gmail.com","password":"pass12345678"}'
curl.exe -i -H "Authorization: Bearer $($resp.accessToken)" http://localhost:8080/api/payments/customers/<valid-userId>
# Expected: HTTP 200 OK (Returns sensitive Stripe data successfully to the authenticated owner)
```

---

## 3. Vulnerability 3 (C-3): Client-Controlled Payment Amount (Price Manipulation)

### Vulnerability Summary

| Field | Value |
|---|---|
| **ID** | C-3 |
| **CWE** | CWE-20 — Improper Input Validation |
| **OWASP** | A04:2021 – Insecure Design |
| **Service** | `appointmentService` (`CreateAppointmentRequest.java`, `AppointmentServiceImpl.java`) |
| **Status** | ✅ IMPLEMENTED & VERIFIED |

### Affected Component
Prior to remediation, the appointment booking endpoint (`POST /api/appointments`) trusted the client to specify the consultation fee:
* `CreateAppointmentRequest.java` exposed `amount` and `currency` fields.
* `AppointmentServiceImpl.java` stored this client-supplied `amount` directly into the database.
* The subsequent Stripe checkout session generated a payment link based on this unvalidated, attacker-controlled price.

### Root Cause
The backend relied on the client to provide business-critical pricing data rather than determining the price on the server side. Because the DTO included `@NotNull` validation for `amount`, the API actively expected the client to dictate the price, which violates the principle of never trusting user input for sensitive business logic (CWE-20).

### Remediation
1. **Removed Attack Surface:** Removed the `amount` and `currency` fields from the `CreateAppointmentRequest` DTO. Unknown fields sent by older clients are silently ignored by Jackson.
2. **Server-Side Enforcement:** Modified `AppointmentServiceImpl` to assign a strict, server-defined constant (LKR 2500.00) for all appointments, completely disregarding any client-supplied pricing data.
3. **Frontend UI Update:** Updated `BookingForm.jsx` to make the amount and currency `<input>` fields `disabled`, preventing users from attempting to modify the price visually.

### Verification Evidence

**BEFORE Evidence (Live curl tests):**
```powershell
# Authenticated patient books appointment with manipulated amount of 0.01
$body = '{"patientId":"bd22fcee-dee6-47a5-96e8-14649d98d398","doctorId":"<uuid>","slotId":"<uuid>","reason":"Checkup","notes":"","amount":"0.01","currency":"lkr"}'

$result = Invoke-RestMethod -Uri "http://localhost:8080/api/appointments" `
  -Method Post -ContentType "application/json" `
  -Headers @{ Authorization = "Bearer $token" } `
  -Body $body

# Expected BEFORE: $result.amount equals 0.01 (server blindly stored attacker price)
```

**AFTER Evidence (Live curl tests):**
```powershell
# Attacker attempts to inject amount=0.01
$body1 = '{"patientId":"bd22fcee-dee6-47a5-96e8-14649d98d398","doctorId":"<uuid>","slotId":"<new-uuid>","reason":"Checkup","notes":"","amount":"0.01","currency":"lkr"}'

$r1 = Invoke-RestMethod -Uri "http://localhost:8080/api/appointments" `
  -Method Post -ContentType "application/json" `
  -Headers @{ Authorization = "Bearer $token" } `
  -Body $body1

# Expected AFTER: $r1.amount == 2500.00 (attacker input completely ignored)
```
