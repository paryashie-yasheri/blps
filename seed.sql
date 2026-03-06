--liquibase formatted sql
--changeset achievement-service:2026.03.04--1-initial-schema

--rollback DELETE FROM achievement_types WHERE code IN ('first_lesson', 'course_completed', 'lesson_master', 'daily_login', 'streak_7days', 'streak_30days');

INSERT INTO achievement_types (code, name, description, points_awarded, is_repeatable, icon_url) VALUES
('first_lesson', 'Первый урок', 'Завершите свой первый урок', 10, false, '/icons/first_lesson.png'),
('course_completed', 'Курс завершен', 'Завершите курс', 100, false, '/icons/course_completed.png'),
('lesson_master', 'Мастер уроков', 'Завершите 10 уроков', 50, false, '/icons/lesson_master.png'),
('daily_login', 'Ежедневный вход', 'Войдите в систему', 1, true, '/icons/daily_login.png'),
('streak_7days', 'Неделя подряд', '7 дней обучения подряд', 25, false, '/icons/streak_7.png'),
('streak_30days', 'Месяц подряд', '30 дней обучения подряд', 100, false, '/icons/streak_30.png');

