# ClinicMate - Secure Software Development (SE4030) Assignment 01

This repository contains the security-hardened **ClinicMate** healthcare microservices application, prepared for the **SE4030 – Secure Software Development** assignment at **SLIIT**. The project identifies critical security weaknesses through dynamic and static application security testing (DAST via OWASP ZAP, manual code review, and penetration testing), implements industry-standard mitigations, integrates federated Single Sign-On (Google OAuth 2.0 / OpenID Connect Authorization Code Grant), and preserves existing clinical workflows across all microservices.

---

## Group Members & Individual Contributions

| Member Name | Student ID | Assigned Vulnerabilities / Contributions | Branch |
| :--- | :--- | :--- | :--- |
| **Wijesinghe L P P** | **IT22555380** | **Vulnerability 1:** Content Security Policy (CSP) Header Not Set (CWE-693)<br>**Vulnerability 2:** Missing Anti-Clickjacking Header (X-Frame-Options / frame-ancestors) (CWE-1021)<br>**Feature:** Federated Authentication via Google Sign-In (OAuth 2.0 / OIDC Authorization Code Grant) | `feature/panduka` |
| **Darshan R** | **IT22097156** | **Finding 1:** Insecure Password Storage & Predictable Auth Tokens (CWE-256, CWE-287)<br>**Finding 2:** Broken Access Control & Privilege Escalation (CWE-269, CWE-639) | `feature/rd927` |
| _Add Member 3_ | _Add Index Number_ | _Add Contribution_ | _Add Branch_ |
| _Add Member 4_ | _Add Index Number_ | _Add Contribution_ | _Add Branch_ |

---

## Project Links

- **Original Project Repositories (Baseline):** <https://github.com/orgs/CTSEAssignment01/repositories>
- **Modified Secured Repository:** <https://github.com/SLIIT-Y4-ORG/SSD-SE4030-ASSIGNMENT>
- **Demonstration Video:** _[Insert YouTube Demonstration URL - Max 20 Minutes]_

> [!NOTE]
> The baseline repository reflects the original, vulnerable implementation prior to semester start. All security hardening, commits, and tests have been developed on individual feature branches and integrated into this repository.

---

## Security Fixes Overview

### Individual Contribution: Wijesinghe L P P (IT22555380)

#### 1. Vulnerability 1: Content Security Policy (CSP) Header Not Set
- **Risk Level:** Medium (CVSS: 6.5)
- **OWASP Top 10 Category:** A05:2021 – Security Misconfiguration
- **CWE ID:** [CWE-693: Protection Mechanism Failure](https://cwe.mitre.org/data/definitions/693.html)
- **Affected Component:** `frontendClinic` (Nginx Web Server / SPA, `http://localhost:5173`)
- **Vulnerability Description & Impact:**
  The original Nginx configuration served the single-page React application without a `Content-Security-Policy` HTTP header. Without a CSP, browsers cannot differentiate between legitimate application scripts and injected malicious scripts. This left the client-side portal exposed to stored and reflected Cross-Site Scripting (XSS), malicious script injection, data exfiltration, and unauthorized external resource loading.
- **Remediation:**
  Configured an explicit, robust Content Security Policy in `frontendClinic/nginx.conf` that restricts trusted origins for scripts, styles, fonts, images, connections, and frames:
  ```nginx
  add_header Content-Security-Policy "default-src 'self'; script-src 'self' 'unsafe-inline' https://js.stripe.com; style-src 'self' 'unsafe-inline' https://fonts.googleapis.com; font-src 'self' https://fonts.gstatic.com data:; img-src 'self' data: https:; connect-src 'self' http://localhost:8080 http://127.0.0.1:8080 https://api.stripe.com; frame-src 'self' https://js.stripe.com https://hooks.stripe.com; frame-ancestors 'self';" always;
  ```
- **Verification:**
  1. Open Chrome/Edge DevTools (`F12`) $\rightarrow$ **Network** tab $\rightarrow$ request `http://localhost:5173/dashboard`.
  2. Confirm `Content-Security-Policy` header is present in the response headers.
  3. Re-run OWASP ZAP passive scan rule `10038`; the alert is cleared with 0 findings.
- **Git Commit:** `a7d8cb2` (`Fix: Implemented Content Security Policy (CSP) header in Nginx to prevent XSS (CWE-693)`)

---

#### 2. Vulnerability 2: Missing Anti-Clickjacking Header (`X-Frame-Options`)
- **Risk Level:** Medium (CVSS: 5.4)
- **OWASP Top 10 Category:** A05:2021 – Security Misconfiguration
- **CWE ID:** [CWE-1021: Improper Restriction of Rendered UI Layers or Frames](https://cwe.mitre.org/data/definitions/1021.html)
- **Affected Component:** `frontendClinic` (Nginx Web Server, `http://localhost:5173/dashboard`, `/sitemap.xml`)
- **Vulnerability Description & Impact:**
  The frontend web server failed to return anti-framing headers (`X-Frame-Options` and CSP `frame-ancestors`). Consequently, an attacker could embed the ClinicMate portal inside a hidden or transparent `<iframe>` on a malicious third-party website (Clickjacking / UI Redressing). Logged-in users (doctors, patients, administrators) could be deceived into performing inadvertent state changes, such as modifying appointments, releasing slots, or confirming payments.
- **Remediation:**
  Implemented defense-in-depth clickjacking protection in `frontendClinic/nginx.conf` by adding both legacy and modern W3C framing directives:
  ```nginx
  # Anti-Clickjacking Header (CWE-1021)
  add_header X-Frame-Options "SAMEORIGIN" always;
  ```
  Combined with `frame-ancestors 'self'` in the Content Security Policy, modern browsers enforce CSP Level 2/3 frame ancestor rules while older browsers enforce `X-Frame-Options: SAMEORIGIN`.
- **Verification:**
  1. Inspect response headers on any document request (`/`, `/dashboard`, `/sitemap.xml`) to verify `X-Frame-Options: SAMEORIGIN` is returned.
  2. Attempt to embed `http://localhost:5173` inside an external HTML iframe (`<iframe src="http://localhost:5173"></iframe>`); modern browsers refuse to frame the page (`Refused to display in a frame because it set 'X-Frame-Options' to 'SAMEORIGIN'`).
  3. Re-run OWASP ZAP passive scan rule `10020`; the alert is cleared with 0 findings.
- **Git Commit:** `2aedbba` (`Fix: Implemented X-Frame-Options and frame-ancestors to prevent Clickjacking attacks (CWE-1021)`)

---

#### 3. Feature: Federated Authentication via Google Sign-In (OAuth 2.0 / OIDC)
- **Protocol & Standard:** OAuth 2.0 (RFC 6749) & OpenID Connect Core 1.0
- **Grant Type:** Authorization Code Grant (`response_type=code`, `scope=openid email profile`)
- **Affected Components:** `userService` (Backend), `apiGateway` (Routing), `frontendClinic` (SPA), `docker-compose.yaml` (Infrastructure)
- **Summary:**
  Implemented secure, federated Single Sign-On allowing patients to authenticate with their Google accounts. Follows the Authorization Code Grant pattern where authorization codes are exchanged strictly server-side by `userService` using confidential client credentials, preventing token exposure in the browser. Employs cryptographically secure CSRF `state` validation, verifies Google's `email_verified == true` claim, auto-provisions accounts with `ROLE_PATIENT`, and generates secure ClinicMate JWT tokens.
- **Detailed Documentation:** See the dedicated [OAuth 2.0 / OpenID Connect (OIDC) Implementation](#oauth-20--openid-connect-oidc-implementation) section below and [SECURITY_FINDINGS_PANDUKA.md](SECURITY_FINDINGS_PANDUKA.md).

---

### Individual Contribution: Darshan R (IT22097156)

#### 1. Insecure Password Storage & Predictable Authentication Tokens
- **OWASP Category:** A07:2021 – Identification and Authentication Failures
- **CWE ID:** CWE-256, CWE-287
- **Affected Component:** `userService`
- **Mitigation Summary:**
  Replaced plaintext passwords and predictable token IDs with PBKDF2-HMAC-SHA-256 password hashing (600,000 iterations, unique 128-bit salt, 256-bit key) and cryptographically signed, expiring HMAC-SHA-256 tokens with versioned session revocation and refresh token rotation.
- **Documentation:** See [SECURITY_FINDINGS_MEMBER.md](SECURITY_FINDINGS_MEMBER.md) for full test suites and reproduction evidence.

#### 2. Broken Access Control & Privilege Escalation
- **OWASP Category:** A01:2021 – Broken Access Control
- **CWE ID:** CWE-269, CWE-639
- **Affected Components:** `userService`, `doctorService`, `frontendClinic`
- **Mitigation Summary:**
  Restricted public registration to `PATIENT` role only, enforced JWT-based role validation on all user/doctor endpoints, decoupled doctor application approval into an admin-only workflow, and eliminated BOLA/IDOR across schedule and profile management.
- **Documentation:** See [SECURITY_FINDINGS_MEMBER.md](SECURITY_FINDINGS_MEMBER.md).

---

## OAuth 2.0 / OpenID Connect (OIDC) Implementation

The application implements federated single sign-on (SSO) via **Google Sign-In** using the **OAuth 2.0 / OpenID Connect (OIDC) Authorization Code Grant**.

### 1. Architectural Sequence Flow
```
[ User / Browser ]             [ Frontend SPA ]             [ API Gateway / User Service ]           [ Google Identity ]
       |                              |                                    |                                 |
       | 1. Click "Sign in with Google"|                                   |                                 |
       |----------------------------->|                                    |                                 |
       |                              | 2. Generate random state & store   |                                 |
       |                              |    Redirect to Google Auth URL     |                                 |
       | 3. Redirect to Google Auth   |----------------------------------->|                                 |
       |---------------------------------------------------------------------------------------------------->|
       |                              |                                    |                                 |
       | 4. Authenticate & Grant Consent                                   |                                 |
       |---------------------------------------------------------------------------------------------------->|
       |                              |                                    |                                 |
       | 5. Redirect back with ?code=AUTH_CODE&state=STATE                 |                                 |
       |<----------------------------------------------------------------------------------------------------|
       |                              |                                    |                                 |
       | 6. Render /auth/callback     |                                    |                                 |
       |----------------------------->|                                    |                                 |
       |                              | 7. Validate state (CSRF check)     |                                 |
       |                              |    POST /api/auth/google {code}    |                                 |
       |                              |----------------------------------->|                                 |
       |                              |                                    | 8. Server-to-server code        |
       |                              |                                    |    exchange (client_secret)     |
       |                              |                                    |-------------------------------->|
       |                              |                                    | 9. Returns id_token & tokens    |
       |                              |                                    |<--------------------------------|
       |                              |                                    |                                 |
       |                              |                                    | 10. Verify email_verified == true|
       |                              |                                    |     Auto-provision PATIENT user |
       |                              |                                    |     Issue ClinicMate JWT tokens |
       |                              | 11. Return AuthResponse (tokens)   |                                 |
       |                              |<-----------------------------------|                                 |
       | 12. Store in sessionStorage  |                                    |                                 |
       |     Redirect to /dashboard   |                                    |                                 |
       |<-----------------------------|                                    |                                 |
```

### 2. Security Controls & Standards Adherence
- **Confidential Client Credentials:** The Google `client_secret` is retained strictly within the backend `userService` environment configuration. It is never transmitted to or embedded in the frontend bundle, complying with OAuth 2.0 confidential client standards.
- **CSRF Mitigation (State Parameter):** A cryptographically random `state` parameter is generated in the browser before redirecting and strictly verified upon return to `/auth/callback`, neutralizing authorization code injection and login CSRF attacks (CWE-352).
- **Email Verification Guard:** The backend strictly validates the `email_verified == true` claim returned in the Google ID token/UserInfo response before authenticating or provisioning any account.
- **Principle of Least Privilege:** Self-registered Google accounts are assigned the `PATIENT` role by default (`ROLE_PATIENT`). Administrative or Doctor privileges cannot be self-assigned.
- **Isolated Password Field:** Auto-provisioned federated accounts are initialized with high-entropy unguessable password hashes, preventing credential stuffing or empty password logins.
- **Microservices & Database Resilience:** Configured `extra_hosts` for Supabase transaction pooler mapping (`aws-0-ap-south-1.pooler.supabase.com:65.0.195.55`), `ddl-auto: none`, connection pool tuning (`max-pool: 3`, `connection-timeout: 60000`), and container restart policies (`restart: unless-stopped`).

### 3. Key Endpoints & Routes
- **`GET /api/auth/google/url?state={state}&redirectUri={redirectUri}`:** Generates the secure Google OAuth 2.0 authorization URL.
- **`POST /api/auth/google`:** Accepts authorization code and redirect URI, performs server-side token exchange with Google, and returns ClinicMate JWT tokens.
- **`GET /api/auth/google/callback`:** Backend callback redirection handler.
- **`/auth/callback`:** Frontend React callback route displaying animated authentication progress, verifying CSRF state, and routing the user to `/dashboard`.

---

## Architecture & Technology Stack

- **Frontend Client:** React 18, React Router v6, Axios, Vite, Nginx (Alpine Linux) — Port `5173`
- **API Gateway:** Spring Cloud Gateway — Port `8080`
- **Identity & User Service:** Spring Boot 3, Spring Data JPA, PostgreSQL / Supabase — Port `8081`
- **Doctor Service:** Spring Boot 3, Spring Data JPA, PostgreSQL / Supabase — Port `8082`
- **Patient Service:** Spring Boot 3, Spring Data JPA, PostgreSQL / Supabase — Port `8083`
- **Appointment Service:** Spring Boot 3, Spring Data JPA, PostgreSQL / Supabase — Port `8084`
- **Payment Service:** Spring Boot 3, Stripe Checkout Integration, PostgreSQL / Supabase — Port `8085`
- **Container Orchestration:** Docker Engine & Docker Compose v2

---

## How to Run Locally

### Prerequisites
- Docker Engine & Docker Compose (v2.20+)
- Java 17+ & Maven 3.9+ (for running test suites)
- Node.js 20+ & npm (for frontend development)

### Step 1: Clone Repository & Checkout Branch
```bash
git clone https://github.com/SLIIT-Y4-ORG/SSD-SE4030-ASSIGNMENT.git
cd SSD-SE4030-ASSIGNMENT
git checkout feature/panduka
```

### Step 2: Configure Environment Variables
Ensure each service has its corresponding `.env` file configured. Local template files (`.env.example`) are provided in each service folder.
For Google Sign-In, populate your credentials in `userService/.env`:
```env
GOOGLE_CLIENT_ID=894493251184-400gm6crc0ihlq6ogfsbte8mjnrevu2f.apps.googleusercontent.com
GOOGLE_CLIENT_SECRET=your_google_client_secret
GOOGLE_REDIRECT_URI=http://localhost:5173/auth/callback
```

### Step 3: Build & Start All Services
```bash
docker compose up -d --build
```

### Step 4: Verify Container Status
```bash
docker compose ps
```

All services will be accessible at:
- **Web Application Portal:** <http://localhost:5173>
- **API Gateway:** <http://localhost:8080>
- **User Service:** <http://localhost:8081>
- **Doctor Service:** <http://localhost:8082>
- **Patient Service:** <http://localhost:8083>
- **Appointment Service:** <http://localhost:8084>
- **Payment Service:** <http://localhost:8085>

---

## Verification & Evaluator Testing Guide

Evaluators and professors can verify the implemented security fixes and features through the following steps:

### 1. Git Commit History Inspection
To verify separate, descriptive commits per vulnerability fix and feature:
```bash
git log --oneline -n 11
```
**Expected Output:**
```text
8f961af Docs: Document Google OAuth 2.0 implementation and update security findings report
53f80f0 Config(infra): Configure Docker Compose resilience and environment parameters for Google OAuth
b28e35e Feat(frontend): Add Google Sign-In button and styled divider to Login and Register views
f74fd06 Feat(frontend): Add OAuth 2.0 authorization code callback handler and route
1c6e9f1 Feat(frontend): Integrate Google OAuth API client methods and authentication context
bfd672d Feat(backend): Implement Google user authentication, auto-provisioning, and REST endpoints
5214b05 Feat(backend): Implement Google OAuth 2.0 service for authorization URL generation and token exchange
e7077c0 Feat(backend): Add Google OAuth 2.0 configuration properties and DTO models
f79afac docs: Add Panduka group member details
2aedbba Fix: Implemented X-Frame-Options and frame-ancestors to prevent Clickjacking attacks (CWE-1021)
a7d8cb2 Fix: Implemented Content Security Policy (CSP) header in Nginx to prevent XSS (CWE-693)
```

### 2. Verify Security Headers (cURL / HTTP Client)
Run the following cURL command against the frontend service:
```bash
curl -I http://localhost:5173/dashboard
```
**Expected Response Headers:**
```http
HTTP/1.1 200 OK
Server: nginx
Content-Type: text/html
Content-Security-Policy: default-src 'self'; script-src 'self' 'unsafe-inline' https://js.stripe.com; style-src 'self' 'unsafe-inline' https://fonts.googleapis.com; font-src 'self' https://fonts.gstatic.com data:; img-src 'self' data: https:; connect-src 'self' http://localhost:8080 http://127.0.0.1:8080 https://api.stripe.com; frame-src 'self' https://js.stripe.com https://hooks.stripe.com; frame-ancestors 'self';
X-Frame-Options: SAMEORIGIN
```

### 3. Verify Google OAuth Endpoint
Verify that the Google authorization URL generation endpoint is functional through the API Gateway:
```bash
curl "http://localhost:8080/api/auth/google/url?state=testState&redirectUri=http://localhost:5173/auth/callback"
```
**Expected Response (HTTP 200 OK):**
```json
{
  "url": "https://accounts.google.com/o/oauth2/v2/auth?client_id=894493251184-400gm6crc0ihlq6ogfsbte8mjnrevu2f.apps.googleusercontent.com&redirect_uri=http://localhost:5173/auth/callback&response_type=code&scope=openid%20email%20profile&state=testState&access_type=offline&prompt=select_account"
}
```

### 4. Automated Backend Test Suites
Run the backend security regression tests:
```bash
# User Service Unit and Security Tests
cd userService && mvn test
```
**Results:** `23 tests run, 0 failures, 0 errors` (includes Google OAuth authorization URL encoding, code exchange mocks, and security validation).

### 5. End-to-End User Sign-In Flow
1. Navigate to `http://localhost:5173/login` in any modern web browser.
2. Click the **"Sign in with Google"** button.
3. Authenticate with your Google account and grant consent.
4. Google redirects to `http://localhost:5173/auth/callback?code=...&state=...`.
5. The callback page validates the CSRF `state`, exchanges the authorization code through `userService`, stores the issued JWT tokens, and redirects into the patient dashboard (`/dashboard`).

---

## Submission Deliverables

- **GitHub Repository:** Complete commit history with isolated, descriptive commits per vulnerability fix and feature.
- **Security Findings Documentation:** Detailed reproduction, mitigation, and verification steps in [SECURITY_FINDINGS_PANDUKA.md](SECURITY_FINDINGS_PANDUKA.md) and [SECURITY_FINDINGS_MEMBER.md](SECURITY_FINDINGS_MEMBER.md).
- **PDF Assessment Report:** Individual and group contributions, OWASP risk evaluation, before/after vulnerability evidence, DAST scan logs, and preventive engineering practices.
- **Demonstration Video:** Video recording showcasing vulnerability reproduction, security header enforcement, and live Google Sign-In OAuth flow.
