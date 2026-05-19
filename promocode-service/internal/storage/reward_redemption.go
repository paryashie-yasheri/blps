package storage

import (
	"context"
	"fmt"

	"github.com/redis/go-redis/v9"

	"promocode-service/internal/config"
	"promocode-service/internal/events"
)

const RewardRedemptionKeyPrefix = "promocode:reward-redemption:"

type RewardRedemptionRecord struct {
	Message events.RewardRedemptionMessage
	Hash    string
}

type RewardRedemptionStore interface {
	SaveRewardRedemption(ctx context.Context, record RewardRedemptionRecord) error
}

type RedisRewardRedemptionStore struct {
	client *redis.Client
}

func NewRedisRewardRedemptionStore(cfg config.RedisConfig) *RedisRewardRedemptionStore {
	return &RedisRewardRedemptionStore{
		client: redis.NewClient(&redis.Options{
			Addr:     cfg.Addr,
			Password: cfg.Password,
			DB:       cfg.DB,
		}),
	}
}

func (s *RedisRewardRedemptionStore) Ping(ctx context.Context) error {
	return s.client.Ping(ctx).Err()
}

func (s *RedisRewardRedemptionStore) Close() error {
	return s.client.Close()
}

func (s *RedisRewardRedemptionStore) SaveRewardRedemption(ctx context.Context, record RewardRedemptionRecord) error {
	operationID := record.Message.EffectiveOperationID()
	if operationID == "" {
		return fmt.Errorf("missing operationId")
	}

	key := RewardRedemptionKey(operationID)
	fields := map[string]any{
		"hash":         record.Hash,
		"json":         record.Message.RawJSON,
		"eventType":    record.Message.EventType,
		"messageKey":   record.Message.Key,
		"operationId":  operationID,
		"userId":       record.Message.Event.UserID,
		"rewardId":     record.Message.Event.RewardID,
		"userRewardId": record.Message.Event.UserRewardID,
		"occurredAt":   record.Message.Event.OccurredAt,
	}

	if err := s.client.HSet(ctx, key, fields).Err(); err != nil {
		return fmt.Errorf("save reward redemption to redis key=%s: %w", key, err)
	}

	return nil
}

func RewardRedemptionKey(operationID string) string {
	return RewardRedemptionKeyPrefix + operationID
}
