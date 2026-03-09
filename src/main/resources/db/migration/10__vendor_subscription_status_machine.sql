ALTER TABLE vendors
    ADD COLUMN IF NOT EXISTS subscription_status VARCHAR(32),
    ADD COLUMN IF NOT EXISTS subscription_started_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS next_billing_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS external_customer_id VARCHAR(255);

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_name = 'vendors'
          AND column_name = 'status_assinatura'
    ) THEN

        UPDATE vendors
        SET subscription_status = CASE LOWER(COALESCE(status_assinatura, 'trial'))
            WHEN 'ativa' THEN 'ATIVA'
            WHEN 'inadimplente' THEN 'INADIMPLENTE'
            WHEN 'suspensa' THEN 'SUSPENSA'
            WHEN 'cancelada' THEN 'CANCELADA'
            WHEN 'expirada' THEN 'EXPIRADA'
            WHEN 'inativa' THEN 'SUSPENSA'
            ELSE 'TRIAL'
        END
        WHERE subscription_status IS NULL;

    ELSE

        UPDATE vendors
        SET subscription_status = 'TRIAL'
        WHERE subscription_status IS NULL;

    END IF;
END $$;

ALTER TABLE vendors
    ALTER COLUMN subscription_status SET DEFAULT 'TRIAL';

ALTER TABLE vendors
    ALTER COLUMN subscription_status SET NOT NULL;