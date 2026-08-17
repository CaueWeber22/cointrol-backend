CREATE TABLE finance.accounts (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    name VARCHAR(100) NOT NULL,
    type VARCHAR(20) NOT NULL,
    currency CHAR(3) NOT NULL,
    status VARCHAR(10) NOT NULL DEFAULT 'ACTIVE',
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_accounts_user
        FOREIGN KEY (user_id) REFERENCES access.users (id) ON DELETE RESTRICT,
    CONSTRAINT uk_accounts_owner UNIQUE (id, user_id),
    CONSTRAINT ck_accounts_name CHECK (name = TRIM(name) AND LENGTH(name) BETWEEN 1 AND 100),
    CONSTRAINT ck_accounts_type CHECK (type IN ('CHECKING', 'SAVINGS', 'CASH', 'INVESTMENT')),
    CONSTRAINT ck_accounts_currency CHECK (currency = UPPER(currency) AND currency ~ '^[A-Z]{3}$'),
    CONSTRAINT ck_accounts_status CHECK (status IN ('ACTIVE', 'ARCHIVED')),
    CONSTRAINT ck_accounts_dates CHECK (updated_at >= created_at)
);

CREATE UNIQUE INDEX uk_accounts_active_name
    ON finance.accounts (user_id, LOWER(name))
    WHERE status = 'ACTIVE';

CREATE INDEX idx_accounts_user_status
    ON finance.accounts (user_id, status, name, id);
