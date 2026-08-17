CREATE TABLE finance.financial_entries (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    account_id UUID NOT NULL,
    category_id UUID,
    type VARCHAR(20) NOT NULL,
    amount NUMERIC(19,4) NOT NULL,
    status VARCHAR(10) NOT NULL,
    effective_date DATE NOT NULL,
    description VARCHAR(255),
    idempotency_key VARCHAR(100),
    request_fingerprint VARCHAR(64),
    version BIGINT NOT NULL DEFAULT 0,
    canceled_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_entries_user
        FOREIGN KEY (user_id) REFERENCES access.users (id) ON DELETE RESTRICT,
    CONSTRAINT fk_entries_account_owner
        FOREIGN KEY (account_id, user_id) REFERENCES finance.accounts (id, user_id) ON DELETE RESTRICT,
    CONSTRAINT fk_entries_category_owner
        FOREIGN KEY (category_id, user_id) REFERENCES finance.categories (id, user_id) ON DELETE RESTRICT,
    CONSTRAINT ck_entries_type CHECK (
        type IN ('INCOME', 'EXPENSE', 'OPENING_BALANCE', 'TRANSFER_IN', 'TRANSFER_OUT')
    ),
    CONSTRAINT ck_entries_amount CHECK (amount > 0),
    CONSTRAINT ck_entries_status CHECK (status IN ('PENDING', 'CLEARED', 'CANCELED')),
    CONSTRAINT ck_entries_category CHECK (
        (type IN ('INCOME', 'EXPENSE') AND category_id IS NOT NULL)
        OR (type IN ('OPENING_BALANCE', 'TRANSFER_IN', 'TRANSFER_OUT') AND category_id IS NULL)
    ),
    CONSTRAINT ck_entries_idempotency CHECK (
        (idempotency_key IS NULL AND request_fingerprint IS NULL)
        OR (idempotency_key IS NOT NULL AND request_fingerprint IS NOT NULL)
    ),
    CONSTRAINT ck_entries_fingerprint CHECK (
        request_fingerprint IS NULL OR request_fingerprint ~ '^[0-9a-f]{64}$'
    ),
    CONSTRAINT ck_entries_cancelation CHECK (
        (status = 'CANCELED' AND canceled_at IS NOT NULL)
        OR (status <> 'CANCELED' AND canceled_at IS NULL)
    ),
    CONSTRAINT ck_entries_dates CHECK (updated_at >= created_at)
);

CREATE UNIQUE INDEX uk_entries_idempotency
    ON finance.financial_entries (user_id, idempotency_key)
    WHERE idempotency_key IS NOT NULL;

CREATE INDEX idx_entries_user_date
    ON finance.financial_entries (user_id, effective_date DESC, created_at DESC, id DESC);

CREATE INDEX idx_entries_user_account_date
    ON finance.financial_entries (user_id, account_id, effective_date DESC, status);

CREATE INDEX idx_entries_user_category
    ON finance.financial_entries (user_id, category_id, effective_date DESC)
    WHERE category_id IS NOT NULL;
