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
