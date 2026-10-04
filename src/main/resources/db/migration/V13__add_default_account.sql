ALTER TABLE finance.accounts
    ADD COLUMN is_default BOOLEAN NOT NULL DEFAULT FALSE;

WITH ranked_accounts AS (
    SELECT id,
           ROW_NUMBER() OVER (PARTITION BY user_id ORDER BY created_at, id) AS account_order
    FROM finance.accounts
)
UPDATE finance.accounts account
SET is_default = TRUE
FROM ranked_accounts ranked
WHERE account.id = ranked.id
  AND ranked.account_order = 1;

CREATE UNIQUE INDEX uk_accounts_default_per_user
    ON finance.accounts (user_id)
    WHERE is_default = TRUE;
