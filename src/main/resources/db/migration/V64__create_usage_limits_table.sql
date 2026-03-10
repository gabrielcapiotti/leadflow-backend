-- V64: Criar tabela usage_limits para controle consolidado de consumo

CREATE TABLE public.usage_limits (
    id BIGSERIAL PRIMARY KEY,
    tenant_id UUID NOT NULL UNIQUE,
    leads_used INTEGER NOT NULL DEFAULT 0,
    users_used INTEGER NOT NULL DEFAULT 0,
    ai_executions_used INTEGER NOT NULL DEFAULT 0,
    plan_id BIGINT NOT NULL,
    
    CONSTRAINT fk_usage_limit_tenant FOREIGN KEY (tenant_id) REFERENCES public.vendors(id) ON DELETE CASCADE,
    CONSTRAINT fk_usage_limit_plan FOREIGN KEY (plan_id) REFERENCES public.plans(id) ON DELETE RESTRICT,
    CONSTRAINT chk_leads_used_positive CHECK (leads_used >= 0),
    CONSTRAINT chk_users_used_positive CHECK (users_used >= 0),
    CONSTRAINT chk_ai_executions_used_positive CHECK (ai_executions_used >= 0)
);

-- Índice para consultas por tenant
CREATE INDEX idx_usage_limit_tenant_id ON public.usage_limits(tenant_id);

-- Índice para consultas por plano
CREATE INDEX idx_usage_limit_plan_id ON public.usage_limits(plan_id);

-- Comentários para documentação
COMMENT ON TABLE public.usage_limits IS 'Controla consumo total consolidado de recursos por tenant, vinculado ao plano contratado';
COMMENT ON COLUMN public.usage_limits.tenant_id IS 'Referência ao vendor (tenant)';
COMMENT ON COLUMN public.usage_limits.leads_used IS 'Total de leads utilizados pelo tenant';
COMMENT ON COLUMN public.usage_limits.users_used IS 'Total de usuários utilizados pelo tenant';
COMMENT ON COLUMN public.usage_limits.ai_executions_used IS 'Total de execuções de IA utilizadas pelo tenant';
