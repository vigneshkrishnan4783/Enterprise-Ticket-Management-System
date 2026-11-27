CREATE TABLE IF NOT EXISTS ticket_lifecycle_policies (
    id BIGSERIAL PRIMARY KEY,
    policy_name VARCHAR(100) NOT NULL UNIQUE,
    organization_id BIGINT REFERENCES organizations(id) ON DELETE CASCADE,
    allow_customer_reopen BOOLEAN NOT NULL DEFAULT TRUE,
    reopen_window_hours INT NOT NULL DEFAULT 72,
    max_reopen_count INT NOT NULL DEFAULT 3,
    auto_close_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    auto_close_after_hours INT NOT NULL DEFAULT 72,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);

INSERT INTO ticket_lifecycle_policies (
    policy_name,
    organization_id,
    allow_customer_reopen,
    reopen_window_hours,
    max_reopen_count,
    auto_close_enabled,
    auto_close_after_hours,
    is_active
)
SELECT 'DEFAULT_POLICY', NULL, TRUE, 72, 3, TRUE, 72, TRUE
WHERE NOT EXISTS (
    SELECT 1 FROM ticket_lifecycle_policies WHERE policy_name = 'DEFAULT_POLICY'
);

ALTER TABLE tickets
    ADD COLUMN IF NOT EXISTS reopen_count INT NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS closed_at TIMESTAMP WITH TIME ZONE;
