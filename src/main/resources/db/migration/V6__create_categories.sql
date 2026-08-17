CREATE TABLE finance.categories (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    name VARCHAR(100) NOT NULL,
    kind VARCHAR(10) NOT NULL,
    status VARCHAR(10) NOT NULL DEFAULT 'ACTIVE',
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_categories_user
        FOREIGN KEY (user_id) REFERENCES access.users (id) ON DELETE RESTRICT,
    CONSTRAINT uk_categories_owner UNIQUE (id, user_id),
    CONSTRAINT ck_categories_name CHECK (name = TRIM(name) AND LENGTH(name) BETWEEN 1 AND 100),
    CONSTRAINT ck_categories_kind CHECK (kind IN ('INCOME', 'EXPENSE')),
    CONSTRAINT ck_categories_status CHECK (status IN ('ACTIVE', 'ARCHIVED')),
    CONSTRAINT ck_categories_dates CHECK (updated_at >= created_at)
);

CREATE UNIQUE INDEX uk_categories_active_name
    ON finance.categories (user_id, kind, LOWER(name))
    WHERE status = 'ACTIVE';

CREATE INDEX idx_categories_user_kind_status
    ON finance.categories (user_id, kind, status, name, id);
