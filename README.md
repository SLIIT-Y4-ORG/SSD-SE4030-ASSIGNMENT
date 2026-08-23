# ClinicMate Security Improvement Project

This repository contains the modified ClinicMate application prepared for the
SE4030 – Secure Software Development group assignment. The project identifies
security weaknesses in the original application, implements mitigations, and
preserves the application's existing clinical workflows.

## Group Members

| Member name | Index number |
|---|---|
| _Add member 1_ | _Add index number_ |
| _Add member 2_ | _Add index number_ |
| _Add member 3_ | _Add index number_ |
| Darshan R | IT22097156 |

## Project Links

- Original project repositories: https://github.com/orgs/CTSEAssignment01/repositories
- Modified project: https://github.com/SLIIT-Y4-ORG/SSD-SE4030-ASSIGNMENT
- Demonstration video: _Add the YouTube URL (maximum 20 minutes)_

The original-project link must point to the unchanged source and demonstrate
that its last commit predates the semester start date.

## Security Work

The implemented security work includes authentication hardening, password
hashing, signed and expiring tokens, session revocation, server-side role
enforcement, prevention of privilege escalation and insecure direct object
access, protected administrative role management, and a secured doctor
application and approval workflow.

The findings prepared for inclusion in the final PDF report are documented in
[SECURITY_FINDINGS_MEMBER.md](SECURITY_FINDINGS_MEMBER.md). The final report
should include all group vulnerabilities, evidence from the original and fixed
versions, testing evidence, unresolved issues and reasons, preventive software
engineering practices, and the OAuth/OpenID Connect implementation.

## Run Locally

Create the ignored `.env` files required by each service, then run:

```bash
docker compose up -d --build
```

Open the frontend at <http://localhost:5173>. The API gateway is available at
<http://localhost:8080>.

Check the service status with:

```bash
docker compose ps
```

Real credentials and secrets must remain in ignored `.env` files and must not
be committed to this repository.

## Submission

The CourseWeb ZIP submission should contain this README and the final report in
PDF format. Ensure the modified GitHub repository retains the detailed commit
history and that the YouTube link is accessible before submission.
