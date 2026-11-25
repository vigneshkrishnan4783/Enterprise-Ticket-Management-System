-- liquibase formatted sql

-- changeset developer:0.0.5-assignment-rules-and-history-metadata
-- validCheckSum: 9:317b703e9e0f7466be7c0ebbcbfc5a28
-- validCheckSum: 9:4a46f27ffbddf25a5d59850cc7479a09
CREATE TABLE IF NOT EXISTS assignment_rules (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(150) NOT NULL UNIQUE,
    description TEXT,
    priority_order INT NOT NULL DEFAULT 100,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    category_id BIGINT REFERENCES categories(id) ON DELETE SET NULL,
    subcategory_id BIGINT REFERENCES subcategories(id) ON DELETE SET NULL,
    organization_id BIGINT REFERENCES organizations(id) ON DELETE SET NULL,
    site_id BIGINT REFERENCES sites(id) ON DELETE SET NULL,
    impact_id BIGINT REFERENCES impacts(id) ON DELETE SET NULL,
    urgency_id BIGINT REFERENCES urgencies(id) ON DELETE SET NULL,
    priority VARCHAR(50),
    channel VARCHAR(50),
    target_team_id BIGINT NOT NULL REFERENCES teams(id) ON DELETE CASCADE,
    target_agent_id BIGINT REFERENCES users(id) ON DELETE SET NULL,
    reason_template VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

ALTER TABLE ticket_assignment_history ADD COLUMN IF NOT EXISTS assignment_source VARCHAR(50) NOT NULL DEFAULT 'MANUAL';
ALTER TABLE ticket_assignment_history ADD COLUMN IF NOT EXISTS reason TEXT;
ALTER TABLE ticket_assignment_history ADD COLUMN IF NOT EXISTS rule_id BIGINT REFERENCES assignment_rules(id) ON DELETE SET NULL;
ALTER TABLE ticket_assignment_history ALTER COLUMN assigned_by_id DROP NOT NULL;

ALTER TABLE tickets ADD COLUMN IF NOT EXISTS assignment_source VARCHAR(50);
ALTER TABLE tickets ADD COLUMN IF NOT EXISTS assignment_reason TEXT;

CREATE INDEX IF NOT EXISTS idx_assignment_rules_active_order ON assignment_rules(is_active, priority_order);
CREATE INDEX IF NOT EXISTS idx_assignment_history_source ON ticket_assignment_history(assignment_source);

INSERT INTO assignment_rules (name, description, priority_order, is_active, category_id, target_team_id, reason_template) VALUES
    ('Network Incident Routing', 'Routes all network and connectivity incidents to Network Engineering Team', 10, TRUE, 1, 2, 'Auto-routed based on Category: Network & Connectivity')
ON CONFLICT (name) DO NOTHING;

INSERT INTO assignment_rules (name, description, priority_order, is_active, priority, target_team_id, reason_template) VALUES
    ('Critical Severity Escalation', 'Directs all critical priority tickets to Infrastructure & Cloud Ops', 20, TRUE, 'CRITICAL', 3, 'Auto-escalated to Infrastructure team due to CRITICAL priority')
ON CONFLICT (name) DO NOTHING;

--rollback DROP INDEX IF EXISTS idx_assignment_history_source; DROP INDEX IF EXISTS idx_assignment_rules_active_order; ALTER TABLE tickets DROP COLUMN IF EXISTS assignment_reason; ALTER TABLE tickets DROP COLUMN IF EXISTS assignment_source; ALTER TABLE ticket_assignment_history DROP COLUMN IF EXISTS rule_id; ALTER TABLE ticket_assignment_history DROP COLUMN IF EXISTS reason; ALTER TABLE ticket_assignment_history DROP COLUMN IF EXISTS assignment_source; DROP TABLE IF EXISTS assignment_rules;
