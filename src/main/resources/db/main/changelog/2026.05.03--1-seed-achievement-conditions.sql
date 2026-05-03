--liquibase formatted sql
--changeset blps:2026.05.03--1-seed-achievement-conditions

--rollback DELETE FROM achievement_conditions WHERE achievement_type_id IN (SELECT achievement_type_id FROM achievement_types WHERE code IN ('course_joined', 'first_lesson', 'course_completed'));
--rollback DELETE FROM achievement_types WHERE code = 'course_joined';
--rollback UPDATE achievement_types SET is_active = false WHERE code IN ('first_lesson', 'course_completed');

INSERT INTO achievement_types (code, name, description, points_awarded, is_repeatable, icon, is_active)
VALUES ('course_joined', 'Первый курс', 'Запишитесь на курс', 5, false, '/icons/course_joined.png', true)
ON CONFLICT (code) DO NOTHING;

UPDATE achievement_types
SET is_active = true
WHERE code IN ('course_joined', 'first_lesson', 'course_completed');

INSERT INTO achievement_conditions (achievement_type_id, event_type, course_pattern, block_pattern, min_completion)
SELECT achievement_type_id, 'course_enrolled', '*', NULL, NULL
FROM achievement_types
WHERE code = 'course_joined'
  AND NOT EXISTS (
      SELECT 1
      FROM achievement_conditions
      WHERE achievement_conditions.achievement_type_id = achievement_types.achievement_type_id
        AND event_type = 'course_enrolled'
  );

INSERT INTO achievement_conditions (achievement_type_id, event_type, course_pattern, block_pattern, min_completion)
SELECT achievement_type_id, 'lesson_complete', '*', '*', 1.0
FROM achievement_types
WHERE code = 'first_lesson'
  AND NOT EXISTS (
      SELECT 1
      FROM achievement_conditions
      WHERE achievement_conditions.achievement_type_id = achievement_types.achievement_type_id
        AND event_type = 'lesson_complete'
  );

INSERT INTO achievement_conditions (achievement_type_id, event_type, course_pattern, block_pattern, min_completion)
SELECT achievement_type_id, 'course_complete', '*', NULL, 1.0
FROM achievement_types
WHERE code = 'course_completed'
  AND NOT EXISTS (
      SELECT 1
      FROM achievement_conditions
      WHERE achievement_conditions.achievement_type_id = achievement_types.achievement_type_id
        AND event_type = 'course_complete'
  );
