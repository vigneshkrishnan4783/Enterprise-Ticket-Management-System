-- liquibase formatted sql

-- changeset developer:0.0.3-impact-urgency-priority-matrix
-- comment: Configurable impact, urgency, and priority matrix tables and ticket column additions

CREATE TABLE IF NOT EXISTS impacts (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    code VARCHAR(50) NOT NULL UNIQUE,
    level_weight INT NOT NULL,
    description TEXT,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS urgencies (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    code VARCHAR(50) NOT NULL UNIQUE,
    level_weight INT NOT NULL,
    description TEXT,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS priority_matrix (
    id BIGSERIAL PRIMARY KEY,
    impact_id BIGINT NOT NULL REFERENCES impacts(id) ON DELETE CASCADE,
    urgency_id BIGINT NOT NULL REFERENCES urgencies(id) ON DELETE CASCADE,
    priority VARCHAR(50) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_priority_matrix_impact_urgency UNIQUE(impact_id, urgency_id)
);

-- Add impact_id and urgency_id to tickets
ALTER TABLE tickets ADD COLUMN IF NOT EXISTS impact_id BIGINT REFERENCES impacts(id) ON DELETE SET NULL;
ALTER TABLE tickets ADD COLUMN IF NOT EXISTS urgency_id BIGINT REFERENCES urgencies(id) ON DELETE SET NULL;

-- Indexes for performance on filtering & joins
CREATE INDEX IF NOT EXISTS idx_tickets_status ON tickets(status);
CREATE INDEX IF NOT EXISTS idx_tickets_priority ON tickets(priority);
CREATE INDEX IF NOT EXISTS idx_tickets_channel ON tickets(channel);
CREATE INDEX IF NOT EXISTS idx_tickets_organization ON tickets(organization_id);
CREATE INDEX IF NOT EXISTS idx_tickets_site ON tickets(site_id);
CREATE INDEX IF NOT EXISTS idx_tickets_category ON tickets(category_id);
CREATE INDEX IF NOT EXISTS idx_tickets_subcategory ON tickets(subcategory_id);
CREATE INDEX IF NOT EXISTS idx_tickets_impact ON tickets(impact_id);
CREATE INDEX IF NOT EXISTS idx_tickets_urgency ON tickets(urgency_id);
CREATE INDEX IF NOT EXISTS idx_tickets_created_at ON tickets(created_at);

-- Seed Default Configurable Impact records
INSERT INTO impacts (name, code, level_weight, description) VALUES
    ('Extensive / Enterprise', 'IMP_EXTENSIVE', 3, 'Impacting entire organization or critical business service'),
    ('Significant / Departmental', 'IMP_SIGNIFICANT', 2, 'Impacting an entire department or group of users'),
    ('Moderate / Individual', 'IMP_MODERATE', 1, 'Impacting single user or non-critical functionality')
ON CONFLICT (code) DO NOTHING;

-- Seed Default Configurable Urgency records
INSERT INTO urgencies (name, code, level_weight, description) VALUES
    ('Critical / Immediate', 'URG_CRITICAL', 3, 'Service is completely down, work has halted'),
    ('High / Degraded', 'URG_HIGH', 2, 'Service performance severely degraded or work is hampered'),
    ('Low / Minor', 'URG_LOW', 1, 'Inconvenience or cosmetic issue with workaround available')
ON CONFLICT (code) DO NOTHING;

-- Seed Default Priority Matrix mappings (Impact x Urgency -> Priority)
-- Extensive (1) + Critical (1) -> CRITICAL
-- Extensive (1) + High (2)     -> HIGH
-- Extensive (1) + Low (3)      -> MEDIUM
-- Significant (2) + Critical (1) -> HIGH
-- Significant (2) + High (2)     -> MEDIUM
-- Significant (2) + Low (3)      -> LOW
-- Moderate (3) + Critical (1)  -> MEDIUM
-- Moderate (3) + High (2)      -> LOW
-- Moderate (3) + Low (3)       -> LOW
INSERT INTO priority_matrix (impact_id, urgency_id, priority) VALUES
    (1, 1, 'CRITICAL'),
    (1, 2, 'HIGH'),
    (1, 3, 'MEDIUM'),
    (2, 1, 'HIGH'),
    (2, 2, 'MEDIUM'),
    (2, 3, 'LOW'),
    (3, 1, 'MEDIUM'),
    (3, 2, 'LOW'),
    (3, 3, 'LOW')
ON CONFLICT (impact_id, urgency_id) DO NOTHING;

--rollback DROP INDEX IF EXISTS idx_tickets_created_at; DROP INDEX IF EXISTS idx_tickets_urgency; DROP INDEX IF EXISTS idx_tickets_impact; DROP INDEX IF EXISTS idx_tickets_subcategory; DROP INDEX IF EXISTS idx_tickets_category; DROP INDEX IF EXISTS idx_tickets_site; DROP INDEX IF EXISTS idx_tickets_organization; DROP INDEX IF EXISTS idx_tickets_channel; DROP INDEX IF EXISTS idx_tickets_priority; DROP INDEX IF EXISTS idx_tickets_status; ALTER TABLE tickets DROP COLUMN IF EXISTS urgency_id; ALTER TABLE tickets DROP COLUMN IF EXISTS impact_id; DROP TABLE IF EXISTS priority_matrix; DROP TABLE IF EXISTS urgencies; DROP TABLE IF EXISTS impacts;
