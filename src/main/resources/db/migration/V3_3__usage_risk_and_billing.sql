CREATE TABLE IF NOT EXISTS vendor_usage (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  vendor_id UUID NOT NULL,
  quota_type quota_type NOT NULL,
  used INT NOT NULL DEFAULT 0,
  period_start TIMESTAMPTZ NOT NULL,
  period_end TIMESTAMPTZ NOT NULL,
  alert_80_sent BOOLEAN NOT NULL DEFAULT FALSE,
  alert_100_sent BOOLEAN NOT NULL DEFAULT FALSE,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  CONSTRAINT fk_vendor_usage_vendor FOREIGN KEY (vendor_id) REFERENCES vendors(id),
  CONSTRAINT ck_vendor_usage_used_non_negative CHECK (used >= 0),
  CONSTRAINT ck_vendor_usage_period_valid CHECK (period_end > period_start),
  CONSTRAINT uk_vendor_usage_period UNIQUE (vendor_id, quota_type, period_start)
);

CREATE TABLE IF NOT EXISTS vendor_risk_alerts (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  vendor_id UUID NOT NULL,
  score INT NOT NULL,
  level risk_level NOT NULL,
  reason TEXT,
  resolved BOOLEAN NOT NULL DEFAULT FALSE,
  resolved_at TIMESTAMPTZ,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  CONSTRAINT fk_vendor_risk_vendor FOREIGN KEY (vendor_id) REFERENCES vendors(id),
  CONSTRAINT ck_vendor_risk_score_valid CHECK (score BETWEEN 0 AND 100)
);

CREATE TABLE IF NOT EXISTS subscription_history (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  vendor_id UUID NOT NULL,
  previous_status subscription_status,
  new_status subscription_status NOT NULL,
  changed_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  reason VARCHAR(255),
  changed_by VARCHAR(255),
  CONSTRAINT fk_subscription_history_vendor FOREIGN KEY (vendor_id) REFERENCES vendors(id)
);
