ALTER TABLE finance.categories
    ADD COLUMN source VARCHAR(10) NOT NULL DEFAULT 'CUSTOM',
    ADD COLUMN default_key VARCHAR(50);

ALTER TABLE finance.categories
    ADD CONSTRAINT ck_categories_source CHECK (source IN ('DEFAULT', 'CUSTOM')),
    ADD CONSTRAINT ck_categories_default_key_source CHECK (
        (source = 'CUSTOM' AND default_key IS NULL)
        OR (source = 'DEFAULT' AND default_key IS NOT NULL)
    );

CREATE UNIQUE INDEX uk_categories_default_key
    ON finance.categories (user_id, default_key)
    WHERE default_key IS NOT NULL;

WITH default_categories(default_key, name, kind) AS (
    VALUES
        ('INCOME_SALARY', 'Salario', 'INCOME'),
        ('INCOME_FREELANCE', 'Freelance', 'INCOME'),
        ('INCOME_INVESTMENTS', 'Investimentos', 'INCOME'),
        ('INCOME_REFUNDS', 'Reembolsos', 'INCOME'),
        ('INCOME_OTHER', 'Outras receitas', 'INCOME'),
        ('EXPENSE_FOOD', 'Alimentacao', 'EXPENSE'),
        ('EXPENSE_GROCERIES', 'Mercado', 'EXPENSE'),
        ('EXPENSE_HOUSING', 'Moradia', 'EXPENSE'),
        ('EXPENSE_TRANSPORTATION', 'Transporte', 'EXPENSE'),
        ('EXPENSE_HEALTH', 'Saude', 'EXPENSE'),
        ('EXPENSE_EDUCATION', 'Educacao', 'EXPENSE'),
        ('EXPENSE_LEISURE', 'Lazer', 'EXPENSE'),
        ('EXPENSE_BILLS', 'Contas', 'EXPENSE'),
        ('EXPENSE_SHOPPING', 'Compras', 'EXPENSE'),
        ('EXPENSE_SUBSCRIPTIONS', 'Assinaturas', 'EXPENSE'),
        ('EXPENSE_TAXES', 'Impostos', 'EXPENSE'),
        ('EXPENSE_OTHER', 'Outras despesas', 'EXPENSE')
),
missing_defaults AS (
    SELECT
        users.id AS user_id,
        defaults.default_key,
        defaults.name,
        defaults.kind,
        md5(users.id::text || ':' || defaults.default_key) AS hash
    FROM access.users users
    CROSS JOIN default_categories defaults
    WHERE NOT EXISTS (
        SELECT 1
        FROM finance.categories categories
        WHERE categories.user_id = users.id
          AND categories.kind = defaults.kind
          AND lower(categories.name) = lower(defaults.name)
    )
)
INSERT INTO finance.categories (
    id,
    user_id,
    name,
    kind,
    status,
    source,
    default_key,
    version,
    created_at,
    updated_at
)
SELECT
    (
        substring(hash from 1 for 8) || '-' ||
        substring(hash from 9 for 4) || '-' ||
        substring(hash from 13 for 4) || '-' ||
        substring(hash from 17 for 4) || '-' ||
        substring(hash from 21 for 12)
    )::uuid,
    user_id,
    name,
    kind,
    'ACTIVE',
    'DEFAULT',
    default_key,
    0,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
FROM missing_defaults;
