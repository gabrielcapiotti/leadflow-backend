/* ======================================================
   V1__extensions.sql
   PostgreSQL extensions required by the system
   ====================================================== */

-- pgcrypto is required for gen_random_uuid()
CREATE EXTENSION IF NOT EXISTS pgcrypto
    WITH SCHEMA public;