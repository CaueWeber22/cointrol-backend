CREATE TABLE access.login_attempts (
    identifier_hash VARCHAR(64) PRIMARY KEY,
    failed_attempts INTEGER NOT NULL,
    window_started_at TIMESTAMPTZ NOT NULL,
    locked_until TIMESTAMPTZ,
    last_failure_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT ck_login_attempts_hash CHECK (identifier_hash ~ '^[0-9a-f]{64}$'),
    CONSTRAINT ck_login_attempts_count CHECK (failed_attempts > 0),
    CONSTRAINT ck_login_attempts_dates CHECK (
        last_failure_at >= window_started_at
        AND updated_at >= window_started_at
        AND (locked_until IS NULL OR locked_until >= last_failure_at)
    )
);

CREATE INDEX idx_login_attempts_cleanup
    ON access.login_attempts (updated_at);

CREATE TABLE access.security_audit_events (
    id UUID PRIMARY KEY,
    user_id UUID,
    identifier_hash VARCHAR(64),
    event_type VARCHAR(40) NOT NULL,
    client_ip VARCHAR(45) NOT NULL,
    user_agent VARCHAR(255),
    occurred_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_security_audit_user
        FOREIGN KEY (user_id) REFERENCES access.users (id) ON DELETE SET NULL,
    CONSTRAINT ck_security_audit_identifier CHECK (
        identifier_hash IS NULL OR identifier_hash ~ '^[0-9a-f]{64}$'
    ),
    CONSTRAINT ck_security_audit_event CHECK (event_type IN (
        'LOGIN_SUCCESS',
        'LOGIN_FAILURE',
        'LOGIN_BLOCKED',
        'TOKEN_REFRESH_SUCCESS',
        'TOKEN_REFRESH_FAILURE',
        'LOGOUT',
        'RATE_LIMIT_EXCEEDED'
    ))
);

CREATE INDEX idx_security_audit_occurred_at
    ON access.security_audit_events (occurred_at DESC, id);

CREATE INDEX idx_security_audit_user
    ON access.security_audit_events (user_id, occurred_at DESC)
    WHERE user_id IS NOT NULL;

CREATE INDEX idx_security_audit_identifier
    ON access.security_audit_events (identifier_hash, occurred_at DESC)
    WHERE identifier_hash IS NOT NULL;
