# Individual Security Contribution: Vulnerability Remediations & OAuth 2.0 / OIDC Implementation

This document contains report-ready evidence and technical specifications for the vulnerability fixes and secure authentication features completed as **Wijesinghe L P P's (IT22555380)** individual contribution for **SE4030 – Secure Software Development Assignment 01**.

**Member Name:** Wijesinghe L P P  
**Student ID:** IT22555380  
**Feature Branch:** `feature/panduka`  

---

## Finding 1: Content Security Policy (CSP) Header Not Set

- **OWASP Top 10 Category:** A05:2021 – Security Misconfiguration
- **CWE ID:** [CWE-693: Protection Mechanism Failure](https://cwe.mitre.org/data/definitions/693.html)
- **Risk Level:** Medium (CVSS: 6.5)
- **Affected Component:** `frontendClinic` (Nginx Web Server / Single Page Application, `http://localhost:5173`)
- **Original Behavior:**
  When loading any route in the frontend application (e.g., `GET http://localhost:5173/dashboard` or `/login`), the Nginx HTTP response did not include a `Content-Security-Policy` header. The browser operated with default permissive policy, allowing scripts, styles, objects, and framing from arbitrary sources if injected into the DOM.
- **Impact:**
  Without a Content Security Policy, the application provides no browser-enforced boundary against Cross-Site Scripting (XSS) attacks. If an attacker manages to inject script payloads through stored patient records, URL parameters, or third-party dependencies, the browser will execute them unconditionally. This enables session hijacking (access token theft from memory/storage), credential theft, sensitive medical data exfiltration, and defacement.
- **Reproduction Against Original Code:**
  1. Start the frontend service via Docker Compose or Nginx.
  2. Open the application in Google Chrome or Microsoft Edge.
  3. Open DevTools (`F12`), navigate to the **Network** tab, and reload `http://localhost:5173/dashboard`.
  4. Select the primary document request and inspect the **Response Headers**.
  5. The `Content-Security-Policy` header is completely absent.
  6. Run an automated DAST scan using OWASP ZAP; Rule `10038` triggers a Medium alert: *"Content Security Policy (CSP) Header Not Set"*.
- **Fix:**
  Configured an explicit, restrictive Content Security Policy in `frontendClinic/nginx.conf` applied at the server block with the `always` directive:
  ```nginx
  add_header Content-Security-Policy "default-src 'self'; script-src 'self' 'unsafe-inline' https://js.stripe.com; style-src 'self' 'unsafe-inline' https://fonts.googleapis.com; font-src 'self' https://fonts.gstatic.com data:; img-src 'self' data: https:; connect-src 'self' http://localhost:8080 http://127.0.0.1:8080 https://api.stripe.com; frame-src 'self' https://js.stripe.com https://hooks.stripe.com; frame-ancestors 'self';" always;
  ```
  - `default-src 'self'`: Restricts resource fetching to the application's origin by default.
  - `script-src 'self' 'unsafe-inline' https://js.stripe.com`: Allows local bundled scripts, necessary Vite module loaders, and the trusted Stripe checkout library.
  - `style-src 'self' 'unsafe-inline' https://fonts.googleapis.com`: Restricts stylesheets to local bundles and Google Fonts.
  - `font-src 'self' https://fonts.gstatic.com data:`: Restricts web fonts to local origins, Google Fonts CDN, and base64 data URIs.
  - `connect-src 'self' http://localhost:8080 http://127.0.0.1:8080 https://api.stripe.com`: Explicitly restricts fetch/XHR traffic to the backend API Gateway (`:8080`) and payment gateway endpoints.
  - `frame-src 'self' https://js.stripe.com https://hooks.stripe.com`: Permits legitimate Stripe payment element frames.
  - `frame-ancestors 'self'`: Restricts who can frame this application to the same origin.
- **Verification:**
  - **DevTools Inspection:** Response headers for `GET /dashboard` now return the complete `Content-Security-Policy` header.
  - **OWASP ZAP DAST:** Re-scanning `http://localhost:5173` shows zero alerts for rule `10038`.
  - **Functional Testing:** Verified that all routes (Dashboard, Doctors list, Appointment Booking, and Stripe Checkout redirect) continue functioning without CSP violations in the browser console.
- **Git Commit:** `a7d8cb2` (`Fix: Implemented Content Security Policy (CSP) header in Nginx to prevent XSS (CWE-693)`)
- **Preventive Practice:**
  - Establish a secure-by-default HTTP response header baseline across all reverse proxies and gateways.
  - Integrate automated DAST security scanning (such as OWASP ZAP baseline scans) into CI/CD pipelines to block releases lacking mandatory security headers.
  - Adopt strict nonce-based or hash-based CSP models when refactoring inline scripts and styles.

---

## Finding 2: Missing Anti-clickjacking Header (`X-Frame-Options`)

- **OWASP Top 10 Category:** A05:2021 – Security Misconfiguration
- **CWE ID:** [CWE-1021: Improper Restriction of Rendered UI Layers or Frames](https://cwe.mitre.org/data/definitions/1021.html)
- **Risk Level:** Medium (CVSS: 5.4)
- **Affected Component:** `frontendClinic` (Nginx Web Server, `http://localhost:5173/dashboard`, `http://localhost:5173/sitemap.xml`)
- **Original Behavior:**
  The original Nginx web server did not send the `X-Frame-Options` header nor the CSP `frame-ancestors` directive on HTTP responses. Any foreign domain could render the entire ClinicMate interface inside an `<iframe>` element.
- **Impact:**
  Enables Clickjacking (UI Redressing) attacks. An attacker creates a deceptive website featuring an invisible `<iframe>` loading `http://localhost:5173/doctors` or an appointment checkout screen over a decoy UI (e.g., "Click here to claim a prize"). When an authenticated user clicks the decoy element, they unintentionally trigger clicks on the underlying ClinicMate UI—authorizing appointment reservations, releasing doctor slots, or altering user profile data without their knowledge.
- **Reproduction Against Original Code:**
  1. Create a local PoC test HTML file on a different port:
     ```html
     <!DOCTYPE html>
     <html>
     <body>
       <h2>Clickjacking Proof of Concept</h2>
       <iframe src="http://localhost:5173/dashboard" width="800" height="600" style="opacity: 0.5;"></iframe>
     </body>
     </html>
     ```
  2. Open the file in a browser. The ClinicMate dashboard successfully renders inside the foreign iframe without browser warnings.
  3. OWASP ZAP passive scan reports rule `10020` under Medium Risk: *"Missing Anti-clickjacking Header"*.
- **Fix:**
  Implemented defense-in-depth clickjacking protection in `frontendClinic/nginx.conf`:
  ```nginx
  # Anti-Clickjacking Header (CWE-1021)
  add_header X-Frame-Options "SAMEORIGIN" always;
  ```
  In conjunction with `frame-ancestors 'self';` inside the `Content-Security-Policy` header:
  - `X-Frame-Options "SAMEORIGIN"` ensures backward-compatibility protection for legacy user agents that do not parse CSP Level 2.
  - `frame-ancestors 'self'` satisfies the modern W3C standard, instructing all compliant modern browsers to deny framing from any foreign origin.
- **Verification:**
  - **DevTools Inspection:** Verified that `X-Frame-Options: SAMEORIGIN` is present in response headers across all document endpoints.
  - **PoC Retest:** Loading the PoC iframe test now triggers the browser error:
    `Refused to display 'http://localhost:5173/' in a frame because it set 'X-Frame-Options' to 'SAMEORIGIN'.`
  - **OWASP ZAP DAST:** Passive scan rule `10020` is verified cleared with 0 alerts.
- **Git Commit:** `2aedbba` (`Fix: Implemented X-Frame-Options and frame-ancestors to prevent Clickjacking attacks (CWE-1021)`)
- **Preventive Practice:**
  - Enforce framing restrictions universally at the reverse proxy / web server layer (`X-Frame-Options: SAMEORIGIN` or `DENY`).
  - Declare `frame-ancestors` in all CSP policies to prevent frame-based embedding.
  - Add automated end-to-end security header checks in pre-deployment test suites.

---

## Feature & Security Implementation: Google Sign-In via OAuth 2.0 / OpenID Connect (OIDC)

- **Standard:** OAuth 2.0 (RFC 6749) & OpenID Connect Core 1.0
- **Grant Type:** Authorization Code Grant (`response_type=code`)
- **Affected Components:** `userService` (Backend), `apiGateway` (Routing), `frontendClinic` (React SPA), `docker-compose.yaml` (Infrastructure)

### 1. Architecture & Protocol Sequence Flow

```
[ User Browser ]                  [ Frontend SPA ]               [ API Gateway / User Service ]           [ Google Identity ]
       |                                 |                                      |                                 |
       | 1. Click "Sign in with Google"  |                                      |                                 |
       |-------------------------------->|                                      |                                 |
       |                                 | 2. GET /api/auth/google/url?state=.. |                                 |
       |                                 |------------------------------------->|                                 |
       |                                 | 3. Returns authorization URL         |                                 |
       |                                 |<-------------------------------------|                                 |
       |                                 | 4. Store state in sessionStorage     |                                 |
       | 5. Redirect to Google Auth      |    window.location.href = authUrl    |                                 |
       |<--------------------------------|                                      |                                 |
       |                                                                        |                                 |
       | 6. Authenticate & Grant Consent with Google Account                    |                                 |
       |--------------------------------------------------------------------------------------------------------->|
       |                                                                                                          |
       | 7. Redirect back to http://localhost:5173/auth/callback?code=AUTH_CODE&state=STATE                       |
       |<---------------------------------------------------------------------------------------------------------|
       |                                 |                                      |                                 |
       | 8. Render /auth/callback        |                                      |                                 |
       |-------------------------------->|                                      |                                 |
       |                                 | 9. Validate state matches stored state|                                |
       |                                 |    POST /api/auth/google {code, uri} |                                 |
       |                                 |------------------------------------->|                                 |
       |                                 |                                      | 10. Server-to-server POST       |
       |                                 |                                      |     https://oauth2.googleapis   |
       |                                 |                                      |     /token (client_secret)      |
       |                                 |                                      |-------------------------------->|
       |                                 |                                      | 11. Returns id_token & tokens   |
       |                                 |                                      |<--------------------------------|
       |                                 |                                      |                                 |
       |                                 |                                      | 12. Parse & verify id_token     |
       |                                 |                                      |     Assert email_verified==true |
       |                                 |                                      |     Find or auto-provision user |
       |                                 |                                      |     Assign ROLE_PATIENT         |
       |                                 |                                      |     Generate ClinicMate JWT     |
       |                                 | 13. Returns AuthResponse (JWT tokens)|                                 |
       |                                 |<-------------------------------------|                                 |
       |                                 | 14. Save tokens to sessionStorage    |                                 |
       | 15. Redirect to /dashboard      |     navigate('/dashboard')           |                                 |
       |<--------------------------------|                                      |                                 |
```

### 2. Threat Modeling & Applied Security Controls

| Threat Vector | CWE / OWASP Reference | Risk | Implemented Security Control |
| :--- | :--- | :--- | :--- |
| **Token Exposure in Browser History / Referer** | CWE-598, CWE-200 | High | Used **Authorization Code Grant** instead of deprecated Implicit Grant. Access and ID tokens are never exposed in URL hash fragments. |
| **Hardcoded Secrets & Secret Leakage** | CWE-798 (Hard-coded Credentials) | Critical | The Google `client_secret` is stored **strictly** on the backend in gitignored `.env` (`GOOGLE_CLIENT_SECRET`). It is never bundled into client JS and complies with GitHub Push Protection. |
| **Cross-Site Request Forgery (Login CSRF)** | CWE-352 (CSRF) | High | Client generates a cryptographically random `state` nonce stored in `sessionStorage`. Upon callback, the state is strictly validated before exchanging the code. |
| **Identity Spoofing via Unverified Emails** | CWE-287 (Improper Authentication) | High | The backend verifies the `email_verified == true` claim from Google's OpenID Connect payload before authenticating or provisioning any account. |
| **Privilege Escalation via Federated Login** | CWE-269 (Privilege Management) | Critical | Auto-provisioned Google users are strictly assigned `ROLE_PATIENT`. Doctor and Admin privileges cannot be obtained through self-registration. |
| **Credential Stuffing on Federated Accounts** | CWE-521 (Weak Password Requirements) | Medium | Auto-provisioned federated users receive a high-entropy unguessable password hash (`UUID.randomUUID()`), preventing empty-password or password-guessing bypasses. |
| **Container & Database Connection Deadlocks** | CWE-400 (Uncontrolled Resource Consumption) | High | Configured `extra_hosts` for Supabase pooler (`aws-0-ap-south-1.pooler.supabase.com:65.0.195.55`), `ddl-auto: none`, connection pool tuning (`max-pool: 3`, `connection-timeout: 60000`), and `restart: unless-stopped`. |

### 3. Implementation Codebase Artifacts

#### Backend (`userService`):
- `com.example.userservice.config.GoogleOAuthProperties`: Type-safe configuration binder for `security.oauth2.google.*`.
- `com.example.userservice.dto.GoogleLoginRequest`: Validated payload containing authorization `code` and `redirectUri`.
- `com.example.userservice.dto.GoogleUserInfo`: Strong typing for verified claims (`sub`, `email`, `email_verified`, `name`, `picture`).
- `com.example.userservice.security.GoogleOAuthService`: Encapsulates Google authorization URL building, server-to-server token exchange, and ID token/UserInfo validation using Spring `RestTemplate`.
- `com.example.userservice.service.AuthServiceImpl`: Handles user lookup, safe patient auto-provisioning, and ClinicMate JWT token generation.
- `com.example.userservice.controller.AuthController`: Exposes `/api/auth/google/url`, `/api/auth/google`, and `/api/auth/google/callback`.

#### Frontend (`frontendClinic`):
- `src/api/index.js`: Added `getGoogleAuthUrl()` and `loginWithGoogle()` API methods.
- `src/context/AuthContext.jsx`: Added `loginWithGoogle()` handler exposing session state.
- `src/pages/OAuthCallbackPage.jsx`: Validates CSRF `state`, exchanges authorization code, displays user feedback during authentication, and redirects to `/dashboard`.
- `src/pages/LoginPage.jsx` & `src/pages/RegisterPage.jsx`: Added dark-themed "Sign in with Google" button with brand SVG icon and styled divider.

### 4. Automated Testing & Verification Evidence

#### Automated Unit Tests (`userService/src/test/java/com/example/userservice/security/GoogleOAuthServiceTest.java`):
- `buildAuthorizationUrl_containsRequiredOAuthParameters()`: Verifies `client_id`, `redirect_uri`, `response_type=code`, and `scope=openid email profile`.
- `exchangeCodeForUserInfo_success()`: Validates token exchange and claims mapping.
- `exchangeCodeForUserInfo_unverifiedEmail_throwsException()`: Asserts rejection when `email_verified` is false.
- `exchangeCodeForUserInfo_tokenEndpointFailure_throwsException()`: Asserts proper error propagation when Google API is unreachable.
- `exchangeCodeForUserInfo_emptyTokens_throwsException()`: Validates handling of invalid responses.
- `exchangeCodeForUserInfo_fallsBackToUserInfoEndpoint()`: Verifies fallback to `userinfo` endpoint when `id_token` is omitted.

**Test Run Result:**
```text
[INFO] Running com.example.userservice.security.GoogleOAuthServiceTest
[INFO] Tests run: 6, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 1.096 s -- in com.example.userservice.security.GoogleOAuthServiceTest
[INFO] Running com.example.userservice.service.AuthServiceSecurityTest
[INFO] Tests run: 7, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.814 s -- in com.example.userservice.service.AuthServiceSecurityTest
[INFO] Results:
[INFO] Tests run: 23, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

#### Manual & cURL Verification:
```bash
# Verify Google Auth URL Generation via API Gateway
curl "http://localhost:8080/api/auth/google/url?state=testState&redirectUri=http://localhost:5173/auth/callback"
```
**Response (HTTP 200 OK):**
```json
{
  "url": "https://accounts.google.com/o/oauth2/v2/auth?client_id=894493251184-400gm6crc0ihlq6ogfsbte8mjnrevu2f.apps.googleusercontent.com&redirect_uri=http://localhost:5173/auth/callback&response_type=code&scope=openid%20email%20profile&state=testState&access_type=offline&prompt=select_account"
}
```

### 5. Git Commit History
```text
8f961af Docs: Document Google OAuth 2.0 implementation and update security findings report
53f80f0 Config(infra): Configure Docker Compose resilience and environment parameters for Google OAuth
b28e35e Feat(frontend): Add Google Sign-In button and styled divider to Login and Register views
f74fd06 Feat(frontend): Add OAuth 2.0 authorization code callback handler and route
1c6e9f1 Feat(frontend): Integrate Google OAuth API client methods and authentication context
bfd672d Feat(backend): Implement Google user authentication, auto-provisioning, and REST endpoints
5214b05 Feat(backend): Implement Google OAuth 2.0 service for authorization URL generation and token exchange
e7077c0 Feat(backend): Add Google OAuth 2.0 configuration properties and DTO models
2aedbba Fix: Implemented X-Frame-Options and frame-ancestors to prevent Clickjacking attacks (CWE-1021)
a7d8cb2 Fix: Implemented Content Security Policy (CSP) header in Nginx to prevent XSS (CWE-693)
```
