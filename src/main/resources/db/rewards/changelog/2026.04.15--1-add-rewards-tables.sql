--liquibase formatted sql
--changeset blps:2026.04.15--1-add-rewards-tables

-- DOWN MIGRATION (rollback)
--rollback DROP TABLE IF EXISTS user_rewards CASCADE;
--rollback DROP TABLE IF EXISTS rewards CASCADE;

CREATE TABLE IF NOT EXISTS rewards (
    reward_id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name               VARCHAR(255) NOT NULL,
    description        TEXT,
    points_cost        INTEGER NOT NULL,
    icon_key           VARCHAR(500),
    is_active          BOOLEAN DEFAULT true,
    quantity           INTEGER,

    CONSTRAINT chk_points_cost_positive CHECK (points_cost > 0),
    CONSTRAINT chk_quantity_positive CHECK (quantity IS NULL OR quantity > 0)
);

CREATE INDEX idx_rewards_active ON rewards(is_active);
CREATE INDEX idx_rewards_points_cost ON rewards(points_cost);

CREATE TABLE IF NOT EXISTS user_rewards (
    user_reward_id     UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    balance_id         UUID NOT NULL,
    reward_id          UUID NOT NULL,
    redeemed_at        TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_user_rewards_balance ON user_rewards(balance_id);
CREATE INDEX idx_user_rewards_reward ON user_rewards(reward_id);
CREATE INDEX idx_user_rewards_redeemed_at ON user_rewards(redeemed_at);
