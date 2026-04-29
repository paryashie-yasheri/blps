--liquibase formatted sql
--changeset blps:2026.04.27--1-achievement-conditions-table

-- DOWN MIGRATION (rollback)
--rollback DROP TABLE IF EXISTS achievement_conditions CASCADE;

CREATE TABLE IF NOT EXISTS achievement_conditions (
    condition_id        UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    achievement_type_id UUID NOT NULL,
    event_type          VARCHAR(100) NOT NULL,
    course_pattern     VARCHAR(255),
    block_pattern      VARCHAR(255),
    min_completion     DOUBLE PRECISION,
    created_at         TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT chk_achievement_conditions_min_completion
        CHECK (min_completion IS NULL OR (min_completion >= 0 AND min_completion <= 1)),

    CONSTRAINT fk_achievement_conditions_type
        FOREIGN KEY (achievement_type_id)
        REFERENCES achievement_types(achievement_type_id) ON DELETE CASCADE
);

CREATE INDEX idx_achievement_conditions_type_id ON achievement_conditions(achievement_type_id);
CREATE INDEX idx_achievement_conditions_event_type ON achievement_conditions(event_type);

UPDATE achievement_types
SET is_active = false
WHERE NOT EXISTS (
    SELECT 1
    FROM achievement_conditions
    WHERE achievement_conditions.achievement_type_id = achievement_types.achievement_type_id
);
