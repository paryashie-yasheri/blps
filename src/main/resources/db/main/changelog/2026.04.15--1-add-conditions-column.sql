--liquibase formatted sql
--changeset blps:2026.04.15--1-add-conditions-column

-- DOWN MIGRATION (rollback)
--rollback ALTER TABLE achievement_types DROP COLUMN IF EXISTS conditions;

ALTER TABLE achievement_types ADD COLUMN IF NOT EXISTS conditions JSONB DEFAULT '{}';
