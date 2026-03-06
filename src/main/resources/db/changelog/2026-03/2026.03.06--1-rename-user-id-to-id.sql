--liquibase formatted sql
--changeset blps:2026.03.05--2-rename-user-id-to-id

-- Rename the primary key column from user_id to id
ALTER TABLE users RENAME COLUMN user_id TO id;

--rollback ALTER TABLE users RENAME COLUMN id TO user_id;