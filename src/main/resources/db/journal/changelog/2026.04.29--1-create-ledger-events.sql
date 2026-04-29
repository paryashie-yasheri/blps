--liquibase formatted sql
--changeset blps:2026.04.29--1-create-ledger-events splitStatements:false

--rollback DROP TABLE IF EXISTS ledger_events CASCADE;
--rollback DROP FUNCTION IF EXISTS reject_ledger_events_updates_and_deletes() CASCADE;

CREATE TABLE IF NOT EXISTS ledger_events (
    event_id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    operation_id     UUID NOT NULL UNIQUE,
    event_type       VARCHAR(64) NOT NULL,
    occurred_at      TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    user_id          UUID NOT NULL,
    balance_id       UUID NOT NULL,
    amount           INTEGER NOT NULL,
    balance_before   INTEGER NOT NULL,
    balance_after    INTEGER NOT NULL,
    reward_id        UUID,
    description      TEXT
);

CREATE INDEX IF NOT EXISTS idx_ledger_events_occurred_at ON ledger_events(occurred_at);
CREATE INDEX IF NOT EXISTS idx_ledger_events_user_id ON ledger_events(user_id);
CREATE INDEX IF NOT EXISTS idx_ledger_events_balance_id ON ledger_events(balance_id);
CREATE INDEX IF NOT EXISTS idx_ledger_events_event_type ON ledger_events(event_type);

CREATE OR REPLACE FUNCTION reject_ledger_events_updates_and_deletes()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN
    RAISE EXCEPTION 'ledger_events is immutable and allows only INSERT';
END;
$$;

DROP TRIGGER IF EXISTS trg_ledger_events_no_update ON ledger_events;
CREATE TRIGGER trg_ledger_events_no_update
BEFORE UPDATE ON ledger_events
FOR EACH ROW
EXECUTE FUNCTION reject_ledger_events_updates_and_deletes();

DROP TRIGGER IF EXISTS trg_ledger_events_no_delete ON ledger_events;
CREATE TRIGGER trg_ledger_events_no_delete
BEFORE DELETE ON ledger_events
FOR EACH ROW
EXECUTE FUNCTION reject_ledger_events_updates_and_deletes();
