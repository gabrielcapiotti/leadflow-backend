CREATE INDEX IF NOT EXISTS idx_users_email ON users (email);
CREATE INDEX IF NOT EXISTS idx_users_role_id ON users (role_id);
CREATE INDEX IF NOT EXISTS idx_users_deleted_at ON users (deleted_at);
CREATE INDEX IF NOT EXISTS idx_users_credentials_updated_at ON users (credentials_updated_at);
CREATE INDEX IF NOT EXISTS idx_users_lock_until ON users (lock_until);

CREATE INDEX IF NOT EXISTS idx_vendors_owner_user_id ON vendors (owner_user_id);
CREATE INDEX IF NOT EXISTS idx_vendors_subscription_status ON vendors (subscription_status);
CREATE INDEX IF NOT EXISTS idx_vendors_deleted_at ON vendors (deleted_at);
CREATE INDEX IF NOT EXISTS idx_vendors_created_at ON vendors (created_at DESC);

CREATE INDEX IF NOT EXISTS idx_tenants_schema_name ON tenants (schema_name);
CREATE INDEX IF NOT EXISTS idx_tenants_deleted_at ON tenants (deleted_at);

CREATE INDEX IF NOT EXISTS idx_vendor_leads_vendor_created ON vendor_leads (vendor_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_vendor_leads_vendor_stage ON vendor_leads (vendor_id, stage);
CREATE INDEX IF NOT EXISTS idx_vendor_leads_vendor_score ON vendor_leads (vendor_id, score DESC);
CREATE INDEX IF NOT EXISTS idx_vendor_leads_deleted_at ON vendor_leads (deleted_at);

CREATE INDEX IF NOT EXISTS idx_stage_history_lead_changed ON vendor_lead_stage_history (vendor_lead_id, changed_at DESC);

CREATE INDEX IF NOT EXISTS idx_vendor_usage_vendor_type_period ON vendor_usage (vendor_id, quota_type, period_start DESC);
CREATE INDEX IF NOT EXISTS idx_vendor_usage_period_end ON vendor_usage (period_end DESC);

CREATE INDEX IF NOT EXISTS idx_vendor_risk_vendor_created ON vendor_risk_alerts (vendor_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_vendor_risk_resolved ON vendor_risk_alerts (resolved);

CREATE INDEX IF NOT EXISTS idx_subscription_history_vendor_changed ON subscription_history (vendor_id, changed_at DESC);

CREATE INDEX IF NOT EXISTS idx_email_events_email_occurred ON email_events (email, occurred_at DESC);
CREATE INDEX IF NOT EXISTS idx_email_events_type_occurred ON email_events (event_type, occurred_at DESC);

CREATE INDEX IF NOT EXISTS idx_vendor_audit_logs_vendor_created ON vendor_audit_logs (vendor_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_vendor_audit_logs_entity ON vendor_audit_logs (entity_type, entidade_id);
CREATE INDEX IF NOT EXISTS idx_vendor_audit_logs_action_created ON vendor_audit_logs (acao, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_user_sessions_token_tenant ON user_sessions (token_id, tenant_id);
CREATE INDEX IF NOT EXISTS idx_user_sessions_user_tenant ON user_sessions (user_id, tenant_id);
CREATE INDEX IF NOT EXISTS idx_user_sessions_active ON user_sessions (active);
CREATE INDEX IF NOT EXISTS idx_user_sessions_last_access ON user_sessions (last_access_at DESC);

CREATE INDEX IF NOT EXISTS idx_refresh_tokens_user_revoked ON refresh_tokens (user_id, revoked);
CREATE INDEX IF NOT EXISTS idx_refresh_tokens_expires_at ON refresh_tokens (expires_at);
CREATE INDEX IF NOT EXISTS idx_refresh_tokens_fingerprint ON refresh_tokens (device_fingerprint);

CREATE INDEX IF NOT EXISTS idx_security_audit_email ON security_audit_logs (email);
CREATE INDEX IF NOT EXISTS idx_security_audit_tenant ON security_audit_logs (tenant);
CREATE INDEX IF NOT EXISTS idx_security_audit_created ON security_audit_logs (created_at DESC);

CREATE INDEX IF NOT EXISTS idx_login_audit_user_tenant ON login_audit (user_id, tenant_id);
CREATE INDEX IF NOT EXISTS idx_login_audit_email_tenant ON login_audit (email, tenant_id);
CREATE INDEX IF NOT EXISTS idx_login_audit_created ON login_audit (created_at DESC);
CREATE INDEX IF NOT EXISTS idx_login_audit_success ON login_audit (success);

CREATE INDEX IF NOT EXISTS idx_password_reset_token_hash ON password_reset_token (token_hash);
CREATE INDEX IF NOT EXISTS idx_password_reset_token_user ON password_reset_token (user_id);
CREATE INDEX IF NOT EXISTS idx_password_reset_token_expires ON password_reset_token (expires_at);
