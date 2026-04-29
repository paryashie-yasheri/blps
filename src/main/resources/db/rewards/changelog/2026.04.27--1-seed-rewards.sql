--liquibase formatted sql
--changeset blps:2026.04.27--1-seed-rewards
--rollback DELETE FROM rewards WHERE name IN ('Tung Tung Tung Sahur', '67', 'Meme Lord', 'Existential Crisis Pack', 'Bug Collector', 'Coffee Machine Harmony');
insert into rewards (name, description, points_cost, icon_key, is_active, quantity) values
('Tung Tung Tung Sahur', 'A mysterious drum that echoes through the void. Beats on its own.', 100, '/icons/tung_tung.png', true, null),
('67', 'Not a reward, it IS the reward. The answer to everything.', 67, '/icons/67.png', true, 1),
('Meme Lord Crown', 'Wearer gains the power to make anyone cringe.', 50, '/icons/meme_crown.png', true, NULL),
('Existential Crisis Pack', 'A gift that keeps on giving. Includes existential dread and a coupon for therapy.', 200, '/icons/crisis.png', true, 3),
('Bug Collector Badge', 'Proves you found at least one bug without screaming.', 30, '/icons/bug.png', true, NULL),
('Coffee Machine Harmony', 'Allows you to peacefully make coffee without passive-aggressive notes.', 75, '/icons/coffee.png', true, NULL);
