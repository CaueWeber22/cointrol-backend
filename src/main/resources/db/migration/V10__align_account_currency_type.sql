ALTER TABLE finance.accounts
    ALTER COLUMN currency TYPE VARCHAR(3)
    USING BTRIM(currency);
