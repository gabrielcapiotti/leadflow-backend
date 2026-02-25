INSERT INTO public.roles (id, name, created_at, updated_at)
VALUES
    (gen_random_uuid(), 'ROLE_USER', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (gen_random_uuid(), 'ROLE_ADMIN', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (name) DO NOTHING;