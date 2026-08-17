CREATE INDEX idx_refresh_tokens_user_id
    ON access.refresh_tokens (user_id);

CREATE INDEX idx_refresh_tokens_expires_at
    ON access.refresh_tokens (expires_at);

CREATE INDEX idx_refresh_tokens_active_user
    ON access.refresh_tokens (user_id, expires_at)
    WHERE revoked_at IS NULL;
