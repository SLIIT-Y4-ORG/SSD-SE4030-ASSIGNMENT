# Individual Security Contribution: Two Vulnerability Fixes

This document contains report-ready evidence for one group member. Replace the member placeholder before submission.

**Member:** `[name and index number]`

## Finding 1: Insecure password storage and forgeable authentication tokens

- **OWASP category:** A07:2021 Identification and Authentication Failures
- **Affected component:** `userService`
- **Original behavior:** Passwords were persisted and compared as plaintext. Login returned a predictable token in the form `token_<user UUID>`. Anyone who learned a user UUID could construct a valid token without authenticating. Refresh accepted and returned an arbitrary caller-provided value. The seeded doctor also used a hard-coded password.
- **Impact:** Database exposure immediately disclosed every password; credential reuse could compromise other systems. Predictable tokens enabled complete account impersonation and inherited-role access to downstream microservices.
- **Reproduction against original code:** Register/login a user, inspect the `users.password` column, then call `/api/auth/validate` with `Authorization: Bearer token_<known-user-uuid>`. The server accepted the constructed token.
- **Fix:** Passwords now use PBKDF2-HMAC-SHA-256 with 210,000 iterations and a unique 128-bit random salt. Existing plaintext records are upgraded at startup. Passwords are write-only in JSON. Access and refresh tokens are separately typed, HMAC-SHA-256 signed, expiry checked, and tamper checked using constant-time comparison. The signing key is required through `SECURITY_TOKEN_SECRET`; the default account was removed.
- **Verification:** `SecurityServicesTest` verifies salted hashing, correct/incorrect password handling, signature tampering, and token-type separation. `AuthServiceSecurityTest` verifies registration persists only a hash.
- **Preventive practice:** Define authentication requirements during threat modelling; use reviewed platform security primitives; prohibit plaintext credentials and hard-coded accounts through review and secret scanning; add negative authentication tests in CI.

## Finding 2: Broken access control and registration privilege escalation

- **OWASP category:** A01:2021 Broken Access Control
- **Affected components:** `userService`, `frontendClinic`
- **Original behavior:** Public registration accepted a client-controlled `role`, including `ADMIN` and `RECEPTIONIST`. All `/api/users` list, lookup, create, update, and delete operations were callable without authentication. Update also accepted a role field. This combined mass assignment, vertical privilege escalation, and IDOR/BOLA.
- **Impact:** An unauthenticated attacker could create an administrator, enumerate personal information, alter another user, promote an account, or delete arbitrary users.
- **Reproduction against original code:** POST `/api/auth/register` with `"role":"ADMIN"`; alternatively call GET/PUT/DELETE `/api/users/<victim-uuid>` without an Authorization header. Both operations succeeded.
- **Fix:** Public signup always assigns `PATIENT` and generates the server-side ID. The privileged-role selector was removed from the UI. Every user-management route now requires a signed bearer token. Collection access and account creation are admin-only; record access is owner-or-admin; self-update cannot change role. Unauthorized and forbidden requests return 401 and 403 respectively.
- **Verification:** `AuthServiceSecurityTest` proves an attempted admin signup becomes a patient. `UserControllerAuthorizationTest` proves cross-account reads are rejected and self-updates cannot enable role changes.
- **Preventive practice:** Maintain an endpoint authorization matrix, deny by default, authorize every object using the authenticated subject, avoid binding persistence entities directly to untrusted privilege fields, and test horizontal and vertical authorization failures.

## Test command and result

Run `mvn test` inside `userService`. Current result: **5 tests run, 0 failures, 0 errors**.

## Deployment note

Create a strong secret before deployment. For Cloud Run, the build configuration expects a Secret Manager entry named `user-service-token-secret`. Local environments must set `SECURITY_TOKEN_SECRET` to a random value containing at least 32 characters.
