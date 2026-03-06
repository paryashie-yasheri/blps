--liquibase formatted sql
--changeset achievement-service:2026.03.04--1-initial-schema

-- ============================================================================
-- DOWN MIGRATION (rollback)
-- ============================================================================
--rollback DROP TABLE IF EXISTS balance_transactions CASCADE;
--rollback DROP TABLE IF EXISTS user_achievements CASCADE;
--rollback DROP TABLE IF EXISTS achievement_types CASCADE;
--rollback DROP TABLE IF EXISTS user_balances CASCADE;
--rollback DROP TABLE IF EXISTS users CASCADE;

-- ============================================================================
-- UP MIGRATION (apply)
-- ============================================================================

-- Drop tables in reverse order of dependencies (for clean re-runs)
DROP TABLE IF EXISTS balance_transactions CASCADE;
DROP TABLE IF EXISTS user_achievements CASCADE;
DROP TABLE IF EXISTS achievement_types CASCADE;
DROP TABLE IF EXISTS user_balances CASCADE;
DROP TABLE IF EXISTS users CASCADE;

CREATE TABLE users (
    user_id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    username        VARCHAR(255) UNIQUE NOT NULL,
    password        VARCHAR(255) NOT NULL,
);

CREATE INDEX idx_users_username ON users(username);

CREATE TABLE user_balances (
    balance_id      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID NOT NULL UNIQUE,
    current_balance INTEGER DEFAULT 0 NOT NULL,
    total_earned    INTEGER DEFAULT 0 NOT NULL,
    total_spent     INTEGER DEFAULT 0 NOT NULL,
    version         INTEGER DEFAULT 1,

    CONSTRAINT fk_user_balances_user
        FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    CONSTRAINT chk_balance_non_negative
        CHECK (current_balance >= 0)
);

CREATE INDEX idx_user_balances_user_id ON user_balances(user_id);
CREATE INDEX idx_user_balances_balance ON user_balances(current_balance);

CREATE TABLE achievement_types (
    achievement_type_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code                VARCHAR(100) UNIQUE NOT NULL,
    name                VARCHAR(255) NOT NULL,
    description         TEXT,
    points_awarded      INTEGER DEFAULT 0 NOT NULL,
    icon                VARCHAR(500),
    is_repeatable       BOOLEAN DEFAULT false,
    is_active           BOOLEAN DEFAULT true,
);

CREATE INDEX idx_achievement_types_code ON achievement_types(code);
CREATE INDEX idx_achievement_types_active ON achievement_types(is_active);

CREATE TABLE user_achievements (
    user_achievement_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    balance_id          UUID NOT NULL,
    achievement_type_id UUID NOT NULL,
    earned_at           TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    reference_id        UUID,
    reference_type      VARCHAR(50),
    is_claimed          BOOLEAN DEFAULT false,
    metadata            JSONB,

    CONSTRAINT fk_user_achievements_balance
        FOREIGN KEY (balance_id) REFERENCES user_balances(balance_id) ON DELETE CASCADE,
    CONSTRAINT fk_user_achievements_type
        FOREIGN KEY (achievement_type_id) REFERENCES achievement_types(achievement_type_id) ON DELETE RESTRICT,
    CONSTRAINT uk_user_achievement_unique
        UNIQUE (balance_id, achievement_type_id, reference_id)
);

CREATE INDEX idx_user_achievements_balance ON user_achievements(balance_id);
CREATE INDEX idx_user_achievements_type ON user_achievements(achievement_type_id);
CREATE INDEX idx_user_achievements_earned_at ON user_achievements(earned_at);
CREATE INDEX idx_user_achievements_claimed ON user_achievements(is_claimed);
CREATE INDEX idx_user_achievements_reference ON user_achievements(reference_id, reference_type);

CREATE TABLE balance_transactions (
    transaction_id      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    balance_id          UUID NOT NULL,
    transaction_type    VARCHAR(50) NOT NULL,
    amount              INTEGER NOT NULL,
    balance_before      INTEGER NOT NULL,
    balance_after       INTEGER NOT NULL,
    user_achievement_id UUID,
    achievement_type_id UUID,
    description         TEXT,
    created_at          TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_balance_transactions_balance
        FOREIGN KEY (balance_id) REFERENCES user_balances(balance_id),
    CONSTRAINT fk_balance_transactions_user_achievement
        FOREIGN KEY (user_achievement_id) REFERENCES user_achievements(user_achievement_id),
    CONSTRAINT fk_balance_transactions_achievement_type
        FOREIGN KEY (achievement_type_id) REFERENCES achievement_types(achievement_type_id)
);

CREATE INDEX idx_balance_transactions_balance_id ON balance_transactions(balance_id);
CREATE INDEX idx_balance_transactions_created_at ON balance_transactions(created_at);
CREATE INDEX idx_balance_transactions_type ON balance_transactions(transaction_type);

