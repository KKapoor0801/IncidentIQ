-- Enable pgcrypto for gen_random_uuid()
CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- ============================================================
-- users
-- ============================================================
CREATE TABLE users (
    id             UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    email          VARCHAR(255) NOT NULL UNIQUE,
    password_hash  VARCHAR(255) NOT NULL,
    full_name      VARCHAR(255) NOT NULL,
    role           VARCHAR(20)  NOT NULL CHECK (role IN ('ADMIN', 'ENGINEER', 'VIEWER')),
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- ============================================================
-- incidents
-- ============================================================
CREATE TABLE incidents (
    id                        UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    title                     VARCHAR(255)  NOT NULL,
    description               TEXT          NOT NULL,
    status                    VARCHAR(20)   NOT NULL DEFAULT 'OPEN'
                              CHECK (status IN ('OPEN', 'IN_PROGRESS', 'RESOLVED', 'CLOSED')),
    priority                  VARCHAR(10)   CHECK (priority IN ('P1', 'P2', 'P3', 'P4')),
    category                  VARCHAR(50),
    ai_resolution_suggestion  TEXT,
    ai_confidence_score       NUMERIC(4,3),
    ai_processed              BOOLEAN       NOT NULL DEFAULT false,
    embedding_vector_id       VARCHAR(64),
    reporter_id               UUID          NOT NULL REFERENCES users(id),
    assignee_id               UUID          REFERENCES users(id),
    created_at                TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at                TIMESTAMPTZ   NOT NULL DEFAULT now(),
    resolved_at               TIMESTAMPTZ,
    version                   INTEGER       NOT NULL DEFAULT 0
);

CREATE INDEX idx_incidents_status     ON incidents(status);
CREATE INDEX idx_incidents_priority   ON incidents(priority);
CREATE INDEX idx_incidents_reporter   ON incidents(reporter_id);
CREATE INDEX idx_incidents_created_at ON incidents(created_at DESC);

-- ============================================================
-- incident_history
-- ============================================================
CREATE TABLE incident_history (
    id             UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    incident_id    UUID         NOT NULL REFERENCES incidents(id),
    field_changed  VARCHAR(100) NOT NULL,
    old_value      TEXT,
    new_value      TEXT,
    changed_by     UUID         NOT NULL REFERENCES users(id),
    changed_at     TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_history_incident_id ON incident_history(incident_id);

-- ============================================================
-- incident_comments
-- ============================================================
CREATE TABLE incident_comments (
    id           UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    incident_id  UUID        NOT NULL REFERENCES incidents(id),
    author_id    UUID        NOT NULL REFERENCES users(id),
    body         TEXT        NOT NULL,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_comments_incident_id ON incident_comments(incident_id);
