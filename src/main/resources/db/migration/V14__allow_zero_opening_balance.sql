ALTER TABLE finance.financial_entries
    DROP CONSTRAINT ck_entries_amount;

ALTER TABLE finance.financial_entries
    ADD CONSTRAINT ck_entries_amount CHECK (
        amount > 0
        OR (type = 'OPENING_BALANCE' AND amount = 0)
    );
