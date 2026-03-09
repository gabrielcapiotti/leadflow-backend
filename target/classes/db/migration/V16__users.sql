/* ======================================================
   USERS TEMPLATE IMPROVEMENTS
   ====================================================== */

ALTER TABLE public.template_users
ADD COLUMN IF NOT EXISTS failed_attempts INTEGER NOT NULL DEFAULT 0;

ALTER TABLE public.template_users
ADD COLUMN IF NOT EXISTS lock_until TIMESTAMPTZ;

ALTER TABLE public.template_users
ADD COLUMN IF NOT EXISTS credentials_updated_at TIMESTAMPTZ;

ALTER TABLE public.template_users
ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMPTZ;


/* ======================================================
   INDEXES
   ====================================================== */

CREATE INDEX IF NOT EXISTS idx_template_users_deleted_at
    ON public.template_users (deleted_at);

CREATE INDEX IF NOT EXISTS idx_template_users_lock_until
    ON public.template_users (lock_until);