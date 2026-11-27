CREATE TABLE IF NOT EXISTS escalation_rules (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    trigger_type VARCHAR(50) NOT NULL,
    priority VARCHAR(20),
    target_level INT NOT NULL DEFAULT 1,
    target_team_id BIGINT REFERENCES teams(id) ON DELETE SET NULL,
    target_agent_id BIGINT REFERENCES users(id) ON DELETE SET NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS ticket_escalation_history (
    id BIGSERIAL PRIMARY KEY,
    ticket_id BIGINT NOT NULL REFERENCES tickets(id) ON DELETE CASCADE,
    rule_id BIGINT REFERENCES escalation_rules(id) ON DELETE SET NULL,
    from_level INT NOT NULL DEFAULT 0,
    to_level INT NOT NULL DEFAULT 1,
    reason TEXT NOT NULL,
    escalated_by BIGINT REFERENCES users(id) ON DELETE SET NULL,
    previous_team_id BIGINT REFERENCES teams(id) ON DELETE SET NULL,
    new_team_id BIGINT REFERENCES teams(id) ON DELETE SET NULL,
    previous_agent_id BIGINT REFERENCES users(id) ON DELETE SET NULL,
    new_agent_id BIGINT REFERENCES users(id) ON DELETE SET NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);

ALTER TABLE tickets ADD COLUMN IF NOT EXISTS escalation_level INT DEFAULT 0 NOT NULL;
ALTER TABLE tickets ADD COLUMN IF NOT EXISTS is_escalated BOOLEAN DEFAULT FALSE NOT NULL;

CREATE INDEX IF NOT EXISTS idx_escalation_history_ticket ON ticket_escalation_history(ticket_id, created_at ASC);
CREATE INDEX IF NOT EXISTS idx_escalation_rules_active ON escalation_rules(is_active, trigger_type);
