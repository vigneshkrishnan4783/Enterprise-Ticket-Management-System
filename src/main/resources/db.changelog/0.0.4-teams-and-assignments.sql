-- liquibase formatted sql

-- changeset developer:0.0.4-teams-and-assignments
-- comment: Teams, team members, ticket assignment columns, and assignment audit history

CREATE TABLE IF NOT EXISTS teams (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    code VARCHAR(50) NOT NULL UNIQUE,
    description TEXT,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS team_members (
    team_id BIGINT NOT NULL REFERENCES teams(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    is_team_lead BOOLEAN NOT NULL DEFAULT FALSE,
    joined_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (team_id, user_id)
);

-- Add assignment columns to tickets table
ALTER TABLE tickets ADD COLUMN IF NOT EXISTS assigned_team_id BIGINT REFERENCES teams(id) ON DELETE SET NULL;
ALTER TABLE tickets ADD COLUMN IF NOT EXISTS assigned_agent_id BIGINT REFERENCES users(id) ON DELETE SET NULL;
ALTER TABLE tickets ADD COLUMN IF NOT EXISTS assigned_at TIMESTAMP WITH TIME ZONE;

-- Assignment Audit History table
CREATE TABLE IF NOT EXISTS ticket_assignment_history (
    id BIGSERIAL PRIMARY KEY,
    ticket_id BIGINT NOT NULL REFERENCES tickets(id) ON DELETE CASCADE,
    previous_team_id BIGINT REFERENCES teams(id) ON DELETE SET NULL,
    new_team_id BIGINT REFERENCES teams(id) ON DELETE SET NULL,
    previous_agent_id BIGINT REFERENCES users(id) ON DELETE SET NULL,
    new_agent_id BIGINT REFERENCES users(id) ON DELETE SET NULL,
    assigned_by_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    comment TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for performance
CREATE INDEX IF NOT EXISTS idx_tickets_assigned_team ON tickets(assigned_team_id);
CREATE INDEX IF NOT EXISTS idx_tickets_assigned_agent ON tickets(assigned_agent_id);
CREATE INDEX IF NOT EXISTS idx_assignment_history_ticket ON ticket_assignment_history(ticket_id);

-- Seed default Support Teams
INSERT INTO teams (name, code, description) VALUES
    ('Tier 1 Helpdesk', 'TEAM_L1_DESK', 'First contact support for general incidents and service requests'),
    ('Network Engineering Team', 'TEAM_NET_OPS', 'Specialized team for routers, firewalls, and network infrastructure'),
    ('Infrastructure & Cloud Ops', 'TEAM_INFRA', 'Core server, database, and virtualization management team')
ON CONFLICT (code) DO NOTHING;

--rollback DROP INDEX IF EXISTS idx_assignment_history_ticket; DROP INDEX IF EXISTS idx_tickets_assigned_agent; DROP INDEX IF EXISTS idx_tickets_assigned_team; DROP TABLE IF EXISTS ticket_assignment_history; ALTER TABLE tickets DROP COLUMN IF EXISTS assigned_at; ALTER TABLE tickets DROP COLUMN IF EXISTS assigned_agent_id; ALTER TABLE tickets DROP COLUMN IF EXISTS assigned_team_id; DROP TABLE IF EXISTS team_members; DROP TABLE IF EXISTS teams;
