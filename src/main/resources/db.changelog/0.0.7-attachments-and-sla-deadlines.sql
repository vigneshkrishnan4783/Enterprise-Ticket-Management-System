-- liquibase formatted sql

-- changeset developer:0.0.7-attachments-and-sla-deadlines
CREATE TABLE IF NOT EXISTS ticket_attachments (
    id BIGSERIAL PRIMARY KEY,
    ticket_id BIGINT NOT NULL REFERENCES tickets(id) ON DELETE CASCADE,
    file_name VARCHAR(255) NOT NULL,
    original_file_name VARCHAR(255) NOT NULL,
    storage_path VARCHAR(500) NOT NULL,
    content_type VARCHAR(100) NOT NULL,
    file_size_bytes BIGINT NOT NULL,
    checksum_sha256 VARCHAR(64) NOT NULL,
    uploaded_by_id BIGINT NOT NULL REFERENCES users(id),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS sla_policies (
    id BIGSERIAL PRIMARY KEY,
    priority VARCHAR(50) NOT NULL UNIQUE,
    response_time_minutes INT NOT NULL,
    resolution_time_minutes INT NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);

ALTER TABLE tickets ADD COLUMN IF NOT EXISTS response_due_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE tickets ADD COLUMN IF NOT EXISTS resolution_due_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE tickets ADD COLUMN IF NOT EXISTS first_responded_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE tickets ADD COLUMN IF NOT EXISTS resolved_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE tickets ADD COLUMN IF NOT EXISTS is_response_breached BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE tickets ADD COLUMN IF NOT EXISTS is_resolution_breached BOOLEAN NOT NULL DEFAULT FALSE;

CREATE INDEX IF NOT EXISTS idx_attachments_ticket_id ON ticket_attachments(ticket_id);
CREATE INDEX IF NOT EXISTS idx_attachments_uploaded_by ON ticket_attachments(uploaded_by_id);
CREATE INDEX IF NOT EXISTS idx_tickets_response_due ON tickets(response_due_at);
CREATE INDEX IF NOT EXISTS idx_tickets_resolution_due ON tickets(resolution_due_at);

INSERT INTO sla_policies (priority, response_time_minutes, resolution_time_minutes) VALUES
    ('CRITICAL', 15, 240),
    ('HIGH', 60, 480),
    ('MEDIUM', 240, 1440),
    ('LOW', 480, 2880)
ON CONFLICT (priority) DO NOTHING;

--rollback DROP INDEX IF EXISTS idx_tickets_resolution_due; DROP INDEX IF EXISTS idx_tickets_response_due; DROP INDEX IF EXISTS idx_attachments_uploaded_by; DROP INDEX IF EXISTS idx_attachments_ticket_id; ALTER TABLE tickets DROP COLUMN IF EXISTS is_resolution_breached; ALTER TABLE tickets DROP COLUMN IF EXISTS is_response_breached; ALTER TABLE tickets DROP COLUMN IF EXISTS resolved_at; ALTER TABLE tickets DROP COLUMN IF EXISTS first_responded_at; ALTER TABLE tickets DROP COLUMN IF EXISTS resolution_due_at; ALTER TABLE tickets DROP COLUMN IF EXISTS response_due_at; DROP TABLE IF EXISTS sla_policies; DROP TABLE IF EXISTS ticket_attachments;
