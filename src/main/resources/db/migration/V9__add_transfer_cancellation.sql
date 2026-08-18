ALTER TABLE finance.transfer_groups
    ADD COLUMN status VARCHAR(10) NOT NULL DEFAULT 'COMPLETED',
    ADD COLUMN cancel_reason VARCHAR(255),
    ADD COLUMN canceled_at TIMESTAMPTZ,
    ADD COLUMN version BIGINT NOT NULL DEFAULT 0,
    ADD COLUMN updated_at TIMESTAMPTZ;

UPDATE finance.transfer_groups
SET updated_at = created_at
WHERE updated_at IS NULL;

ALTER TABLE finance.transfer_groups
    ALTER COLUMN updated_at SET NOT NULL,
    ADD CONSTRAINT ck_transfer_groups_status CHECK (status IN ('COMPLETED', 'CANCELED')),
    ADD CONSTRAINT ck_transfer_groups_cancellation CHECK (
        (status = 'COMPLETED' AND cancel_reason IS NULL AND canceled_at IS NULL)
        OR (status = 'CANCELED' AND cancel_reason IS NOT NULL AND canceled_at IS NOT NULL)
    ),
    ADD CONSTRAINT ck_transfer_groups_dates CHECK (
        updated_at >= created_at AND (canceled_at IS NULL OR canceled_at >= created_at)
    );
