-- Creates the dedicated schema for the patient-service.
-- This runs before Hibernate DDL (ddl-auto: update) so that tables are
-- created inside "patient" schema automatically.
-- Idempotent: safe to run on every application startup.
CREATE SCHEMA IF NOT EXISTS patient;
