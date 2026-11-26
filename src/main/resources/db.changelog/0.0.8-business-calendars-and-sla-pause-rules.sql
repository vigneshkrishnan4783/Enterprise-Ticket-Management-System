-- liquibase formatted sql

-- changeset developer:0.0.8-business-calendars-and-sla-pause-rules
CREATE TABLE IF NOT EXISTS business_calendars (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    time_zone VARCHAR(50) NOT NULL DEFAULT 'UTC',
    work_start_time TIME NOT NULL DEFAULT '09:00:00',
    work_end_time TIME NOT NULL DEFAULT '18:00:00',
    work_days VARCHAR(100) NOT NULL DEFAULT 'MONDAY,TUESDAY,WEDNESDAY,THURSDAY,FRIDAY',
    is_default BOOLEAN NOT NULL DEFAULT FALSE,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS holidays (
    id BIGSERIAL PRIMARY KEY,
    business_calendar_id BIGINT NOT NULL REFERENCES business_calendars(id) ON DELETE CASCADE,
    holiday_date DATE NOT NULL,
    name VARCHAR(100) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT uq_calendar_holiday UNIQUE (business_calendar_id, holiday_date)
);

CREATE TABLE IF NOT EXISTS sla_pause_rules (
    id BIGSERIAL PRIMARY KEY,
    status VARCHAR(50) NOT NULL UNIQUE,
    pauses_response_sla BOOLEAN NOT NULL DEFAULT FALSE,
    pauses_resolution_sla BOOLEAN NOT NULL DEFAULT TRUE,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS ticket_sla_pause_intervals (
    id BIGSERIAL PRIMARY KEY,
    ticket_id BIGINT NOT NULL REFERENCES tickets(id) ON DELETE CASCADE,
    status VARCHAR(50) NOT NULL,
    paused_at TIMESTAMP WITH TIME ZONE NOT NULL,
    resumed_at TIMESTAMP WITH TIME ZONE,
    duration_minutes BIGINT,
    paused_by_id BIGINT REFERENCES users(id),
    resumed_by_id BIGINT REFERENCES users(id),
    pause_reason TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);

ALTER TABLE tickets ADD COLUMN IF NOT EXISTS total_paused_duration_minutes BIGINT NOT NULL DEFAULT 0;
ALTER TABLE tickets ADD COLUMN IF NOT EXISTS is_sla_paused BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE tickets ADD COLUMN IF NOT EXISTS sla_last_paused_at TIMESTAMP WITH TIME ZONE;

CREATE INDEX IF NOT EXISTS idx_holidays_calendar ON holidays(business_calendar_id);
CREATE INDEX IF NOT EXISTS idx_holidays_date ON holidays(holiday_date);
CREATE INDEX IF NOT EXISTS idx_sla_pause_rules_status ON sla_pause_rules(status);
CREATE INDEX IF NOT EXISTS idx_sla_pause_intervals_ticket ON ticket_sla_pause_intervals(ticket_id);
CREATE INDEX IF NOT EXISTS idx_sla_pause_intervals_status ON ticket_sla_pause_intervals(status);

INSERT INTO business_calendars (name, time_zone, work_start_time, work_end_time, work_days, is_default, is_active)
VALUES ('Standard Business Calendar', 'UTC', '09:00:00', '18:00:00', 'MONDAY,TUESDAY,WEDNESDAY,THURSDAY,FRIDAY', TRUE, TRUE)
ON CONFLICT (name) DO NOTHING;

INSERT INTO sla_pause_rules (status, pauses_response_sla, pauses_resolution_sla, is_active)
VALUES
    ('PENDING_CUSTOMER', FALSE, TRUE, TRUE),
    ('PENDING_INTERNAL', FALSE, TRUE, TRUE)
ON CONFLICT (status) DO NOTHING;

--rollback DROP INDEX IF EXISTS idx_sla_pause_intervals_status; DROP INDEX IF EXISTS idx_sla_pause_intervals_ticket; DROP INDEX IF EXISTS idx_sla_pause_rules_status; DROP INDEX IF EXISTS idx_holidays_date; DROP INDEX IF EXISTS idx_holidays_calendar; ALTER TABLE tickets DROP COLUMN IF EXISTS sla_last_paused_at; ALTER TABLE tickets DROP COLUMN IF EXISTS is_sla_paused; ALTER TABLE tickets DROP COLUMN IF EXISTS total_paused_duration_minutes; DROP TABLE IF EXISTS ticket_sla_pause_intervals; DROP TABLE IF EXISTS sla_pause_rules; DROP TABLE IF EXISTS holidays; DROP TABLE IF EXISTS business_calendars;
