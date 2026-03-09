/* ======================================================
   VENDOR LEADS
   Leads associated with vendors
   ====================================================== */

CREATE TABLE IF NOT EXISTS public.vendor_leads (

    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    vendor_id UUID NOT NULL,

    nome_completo VARCHAR(200) NOT NULL,
    whatsapp VARCHAR(30) NOT NULL,

    tipo_consorcio VARCHAR(100),
    valor_credito NUMERIC(15,2),

    urgencia VARCHAR(20),

    stage VARCHAR(50) NOT NULL DEFAULT 'NEW',

    score INT NOT NULL DEFAULT 0,
    owner_email VARCHAR(255),

    observacoes TEXT,

    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP WITH TIME ZONE,

    CONSTRAINT fk_vendor_leads_vendor
        FOREIGN KEY (vendor_id)
        REFERENCES public.vendors(id)
        ON DELETE CASCADE,

    CONSTRAINT chk_vendor_leads_score
        CHECK (score BETWEEN 0 AND 100),

    CONSTRAINT chk_vendor_leads_stage
        CHECK (stage IN ('NEW','CONTACT','PROPOSAL','NEGOTIATION','CLOSED','LOST'))
);


/* ======================================================
   INDEXES
   ====================================================== */

-- Leads by vendor
CREATE INDEX IF NOT EXISTS idx_vendor_leads_vendor
    ON public.vendor_leads (vendor_id);

-- Leads by vendor + stage
CREATE INDEX IF NOT EXISTS idx_vendor_leads_vendor_stage
    ON public.vendor_leads (vendor_id, stage);

-- Leads timeline
CREATE INDEX IF NOT EXISTS idx_vendor_leads_vendor_created
    ON public.vendor_leads (vendor_id, created_at DESC);

-- Filtering by stage only
CREATE INDEX IF NOT EXISTS idx_vendor_leads_stage
    ON public.vendor_leads (stage);