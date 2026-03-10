-- V63: Refatorar tabela subscriptions com plan_id e tenant_id

-- Drop tabela antiga (desenvolvimento/staging - ajustar para produção)
DROP TABLE IF EXISTS public.subscriptions CASCADE;

-- Criar nova estrutura de subscriptions
CREATE TABLE public.subscriptions (
    id BIGSERIAL PRIMARY KEY,
    tenant_id UUID NOT NULL,
    stripe_customer_id VARCHAR(255) NOT NULL,
    stripe_subscription_id VARCHAR(255),
    plan_id BIGINT NOT NULL,
    email VARCHAR(255),
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    started_at TIMESTAMP,
    expires_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_subscription_tenant FOREIGN KEY (tenant_id) REFERENCES public.vendors(id) ON DELETE CASCADE,
    CONSTRAINT fk_subscription_plan FOREIGN KEY (plan_id) REFERENCES public.plans(id) ON DELETE RESTRICT
);

-- Índices para otimizar consultas
CREATE UNIQUE INDEX idx_subscription_stripe_subscription_id ON public.subscriptions(stripe_subscription_id);
CREATE INDEX idx_subscription_stripe_customer_id ON public.subscriptions(stripe_customer_id);
CREATE INDEX idx_subscription_email ON public.subscriptions(email);
CREATE INDEX idx_subscription_tenant_id ON public.subscriptions(tenant_id);
CREATE INDEX idx_subscription_status ON public.subscriptions(status);
