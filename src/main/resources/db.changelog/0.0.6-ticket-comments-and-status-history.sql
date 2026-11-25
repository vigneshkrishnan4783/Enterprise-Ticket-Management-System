-- liquibase formatted sql

-- changeset developer:0.0.6-ticket-comments-and-status-history
CREATE TABLE IF NOT EXISTS ticket_comments (
    id BIGSERIAL PRIMARY KEY,
    ticket_id BIGINT NOT NULL REFERENCES tickets(id) ON DELETE CASCADE,
    author_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    body TEXT NOT NULL,
    is_internal BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS ticket_status_history (
    id BIGSERIAL PRIMARY KEY,
    ticket_id BIGINT NOT NULL REFERENCES tickets(id) ON DELETE CASCADE,
    previous_status VARCHAR(50) NOT NULL,
    new_status VARCHAR(50) NOT NULL,
    changed_by_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    reason TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_comments_ticket_created ON ticket_comments(ticket_id, created_at ASC);
CREATE INDEX IF NOT EXISTS idx_status_history_ticket_created ON ticket_status_history(ticket_id, created_at ASC);

--rollback DROP INDEX IF EXISTS idx_status_history_ticket_created; DROP INDEX IF EXISTS idx_comments_ticket_created; DROP TABLE IF EXISTS ticket_status_history; DROP TABLE IF EXISTS ticket_comments;
