/* ======================================================
   PLANS TABLE - Single Plan Model
   
   Como existe apenas um plano comercial, a tabela
   funciona como configuração fixa do produto.
   ====================================================== */

CREATE TABLE IF NOT EXISTS public.plans (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    max_leads INTEGER NOT NULL,
    max_users INTEGER NOT NULL,
    max_ai_executions INTEGER NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

/* ======================================================
   SEED - Leadflow Standard Plan
   ====================================================== */

INSERT INTO public.plans (name, max_leads, max_users, max_ai_executions, active)
VALUES ('Leadflow Standard', 500, 10, 1000, true)
ON CONFLICT (name) DO NOTHING;

/* ======================================================
   INDEXES
   ====================================================== */

CREATE INDEX IF NOT EXISTS idx_plans_active 
    ON public.plans (active) 
    WHERE active = true;
