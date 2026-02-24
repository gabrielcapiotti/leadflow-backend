/* ======================================================
   SEED DATA FOR ROLES
   ====================================================== */

INSERT INTO public.roles (id, name, created_at, updated_at)
VALUES
    (uuid_generate_v4(), 'ROLE_USER', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (uuid_generate_v4(), 'ROLE_ADMIN', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (name) DO NOTHING;