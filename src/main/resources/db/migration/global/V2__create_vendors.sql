/* ======================================================
   VENDORS (GLOBAL / PUBLIC SCHEMA)
   ====================================================== */

CREATE TABLE IF NOT EXISTS public.vendors (
    id UUID PRIMARY KEY,

    name VARCHAR(255) NOT NULL,

    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Index for vendor lookup by name
CREATE INDEX IF NOT EXISTS idx_vendors_name
    ON public.vendors (name);

-- Index for creation date (useful for analytics / admin dashboards)
CREATE INDEX IF NOT EXISTS idx_vendors_created_at
    ON public.vendors (created_at);