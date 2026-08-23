# Individual Security Contribution: Two Vulnerability Fixes

This document contains report-ready evidence for the two vulnerability fixes completed as Darshan R's individual contribution.

**Member:** Darshan R — IT22097156

## Finding 1: Insecure password storage and forgeable authentication tokens

- **OWASP category:** A07:2021 Identification and Authentication Failures
- **Affected component:** `userService`
- **Original behavior:** Passwords were persisted and compared as plaintext. Login returned a predictable token in the form `token_<user UUID>`. Anyone who learned a user UUID could construct a valid token without authenticating. Refresh accepted and returned an arbitrary caller-provided value. The seeded doctor also used a hard-coded password.
- **Impact:** Database exposure immediately disclosed every password; credential reuse could compromise other systems. Predictable tokens enabled complete account impersonation and inherited-role access to downstream microservices.
- **Reproduction against original code:** Register/login a user, inspect the `users.password` column, then call `/api/auth/validate` with `Authorization: Bearer token_<known-user-uuid>`. The server accepted the constructed token.
- **Fix:** New passwords use PBKDF2-HMAC-SHA-256 with 600,000 iterations, a unique 128-bit random salt, a 256-bit derived key, and a 12–128 character input policy. Existing plaintext and lower-work-factor hashes are upgraded without retaining plaintext. Passwords and token-version fields are write-only in JSON. Access and refresh tokens are separately typed, HMAC-SHA-256 signed, expiry checked, length bounded, and tamper checked using constant-time comparison. Persistent per-user token versions make logout revoke access and refresh tokens; refresh tokens rotate once and replay is rejected under a database lock. Disabled accounts are rejected, missing and incorrect accounts return the same login error, and the signing key is required through `SECURITY_TOKEN_SECRET`.
- **Verification:** `SecurityServicesTest` verifies salts, hashing, legacy migration, response redaction, expiration, signature tampering, malformed-token rejection, and token-type separation. `AuthServiceSecurityTest` verifies patient-only registration, disabled-account rejection, indistinguishable invalid credentials, logout revocation, and refresh-token replay prevention.
- **Preventive practice:** Define authentication requirements during threat modelling; use reviewed platform security primitives; prohibit plaintext credentials and hard-coded accounts through review and secret scanning; add negative authentication tests in CI.

## Finding 2: Broken access control and registration privilege escalation

- **OWASP category:** A01:2021 Broken Access Control
- **Affected components:** `userService`, `doctorService`, `frontendClinic`
- **Original behavior:** Public registration accepted a client-controlled `role`, including `ADMIN` and `RECEPTIONIST`. All `/api/users` list, lookup, create, update, and delete operations were callable without authentication. Update also accepted a role field. This combined mass assignment, vertical privilege escalation, and IDOR/BOLA.
- **Impact:** An unauthenticated attacker could create an administrator, enumerate personal information, alter another user, promote an account, or delete arbitrary users.
- **Reproduction against original code:** POST `/api/auth/register` with `"role":"ADMIN"`; alternatively call GET/PUT/DELETE `/api/users/<victim-uuid>` without an Authorization header. Both operations succeeded.
- **Fix:** Public signup always assigns `PATIENT`, enables the account, resets security-version fields, normalizes email addresses, and generates the server-side ID. The privileged-role selector was removed from public registration. Every user-management route requires a verified bearer token. Collection access, account creation, and the dedicated role-change endpoint are admin-only; record access is owner-or-admin; ordinary profile updates cannot modify role, enabled state, IDs, passwords, or token versions. The final administrator cannot be demoted or deleted. The original doctor-onboarding feature is retained safely: an authenticated patient may submit a pending doctor application, but the backend derives its user ID and email from the validated token and forcibly sets `verified=false` and `active=false`. Only an administrator can approve it. Approval promotes the linked user to `DOCTOR` and then activates the doctor profile, with compensating demotion if profile activation fails. Unverified doctors are excluded from public results and appointment availability, and a doctor can manage only their own verified schedule. Unauthorized and forbidden requests return 401 and 403 respectively.
- **Verification:** `AuthServiceSecurityTest` proves an attempted admin signup becomes a patient. `UserControllerAuthorizationTest` covers cross-account access, missing authentication, non-admin collection access, and self-update authorization. `UserServiceAuthorizationTest` proves mass-assigned role/account-state fields are ignored and the final administrator cannot be deleted. `DoctorApplicationSecurityTest` proves application identity cannot be supplied by the client, applications remain pending, approval promotes the linked user, failed activation compensates the promotion, and doctors cannot manage another doctor's schedule. `AuthHelperTest` checks one-time token validation and correct 401/403 behavior.
- **Preventive practice:** Maintain an endpoint authorization matrix, deny by default, authorize every object using the authenticated subject, avoid binding persistence entities directly to untrusted privilege fields, and test horizontal and vertical authorization failures.

## Test command and result

Run `mvn test` inside both `userService` and `doctorService`, then run `npm run build` inside `frontendClinic`. Current result: **22 backend tests run, 0 failures, 0 errors**, and the frontend production build succeeds.

## Deployment note

Create a strong secret before deployment. For Cloud Run, the build configuration expects a Secret Manager entry named `user-service-token-secret`. Local environments must set `SECURITY_TOKEN_SECRET` to a random value containing at least 32 characters.
