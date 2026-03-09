/* ======================================================
   PAYMENT CHECKOUT REQUESTS
   Stores checkout requests for payment providers
   ====================================================== */

CREATE TABLE IF NOT EXISTS public.payment_checkout_requests (
    id UUID PRIMARY KEY,

    reference_id VARCHAR(255) NOT NULL,
    provider VARCHAR(100) NOT NULL,

    email VARCHAR(150) NOT NULL,

    nome_vendedor VARCHAR(120) NOT NULL,
    whatsapp_vendedor VARCHAR(20) NOT NULL,
    nome_empresa VARCHAR(120),

    slug VARCHAR(80) NOT NULL,

    status VARCHAR(40) NOT NULL,

    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uq_payment_checkout_reference
        UNIQUE (reference_id)
);

-- Lookup by email and status (common for customer support / retries)
CREATE INDEX IF NOT EXISTS idx_payment_checkout_email_status
    ON public.payment_checkout_requests (email, status);

-- Fast lookup by slug (often used in SaaS onboarding flows)
CREATE INDEX IF NOT EXISTS idx_payment_checkout_slug
    ON public.payment_checkout_requests (slug);

-- Index for chronological queries / cleanup jobs
CREATE INDEX IF NOT EXISTS idx_payment_checkout_created_at
    ON public.payment_checkout_requests (created_at);