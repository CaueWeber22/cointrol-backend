CREATE TABLE finance.transfer_groups (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    idempotency_key VARCHAR(100) NOT NULL,
    request_fingerprint VARCHAR(64) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_transfer_groups_user
        FOREIGN KEY (user_id) REFERENCES access.users (id) ON DELETE RESTRICT,
    CONSTRAINT uk_transfer_groups_owner UNIQUE (id, user_id),
    CONSTRAINT uk_transfer_groups_idempotency UNIQUE (user_id, idempotency_key),
    CONSTRAINT ck_transfer_groups_fingerprint CHECK (request_fingerprint ~ '^[0-9a-f]{64}$')
);

ALTER TABLE finance.financial_entries
    ADD COLUMN transfer_group_id UUID;

ALTER TABLE finance.financial_entries
    ADD CONSTRAINT fk_entries_transfer_group_owner
        FOREIGN KEY (transfer_group_id, user_id) REFERENCES finance.transfer_groups (id, user_id) ON DELETE RESTRICT,
    ADD CONSTRAINT ck_entries_transfer_group CHECK (
        (type IN ('TRANSFER_IN', 'TRANSFER_OUT') AND transfer_group_id IS NOT NULL)
        OR (type NOT IN ('TRANSFER_IN', 'TRANSFER_OUT') AND transfer_group_id IS NULL)
    );

CREATE INDEX idx_entries_transfer_group
    ON finance.financial_entries (user_id, transfer_group_id)
    WHERE transfer_group_id IS NOT NULL;
