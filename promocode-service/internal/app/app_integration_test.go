package app_test

import (
	"context"
	"encoding/json"
	"os"
	"strconv"
	"testing"
	"time"

	"github.com/apache/pulsar-client-go/pulsar"
	"github.com/redis/go-redis/v9"

	"promocode-service/internal/app"
	"promocode-service/internal/config"
	"promocode-service/internal/events"
	"promocode-service/internal/storage"
)

func TestPulsarToRedisIntegration(t *testing.T) {
	if os.Getenv("PROMOCODE_INTEGRATION") != "1" {
		t.Skip("set PROMOCODE_INTEGRATION=1 to run integration test")
	}

	ctx, cancel := context.WithTimeout(context.Background(), 30*time.Second)
	defer cancel()

	topic := "persistent://public/default/reward-redemption-integration"
	subscription := "promocode-service-integration"
	operationID := "integration-op-" + strconv.FormatInt(time.Now().UnixNano(), 10)
	userID := "integration-user"

	cfg := config.Config{
		Pulsar: config.PulsarConfig{
			URL:          envOrDefault("PULSAR_URL", "pulsar://localhost:6650"),
			Topic:        topic,
			Subscription: subscription,
		},
		Redis: config.RedisConfig{
			Addr:     envOrDefault("REDIS_ADDR", "localhost:6380"),
			Password: envOrDefault("REDIS_PASSWORD", "redis"),
			DB:       0,
		},
		Workers: 1,
	}

	redisClient := redis.NewClient(&redis.Options{
		Addr:     cfg.Redis.Addr,
		Password: cfg.Redis.Password,
		DB:       cfg.Redis.DB,
	})
	defer redisClient.Close()

	key := storage.RewardRedemptionKey(operationID)
	_ = redisClient.Del(ctx, key).Err()
	defer redisClient.Del(context.Background(), key)

	errCh := make(chan error, 1)
	appCtx, stopApp := context.WithCancel(ctx)
	defer stopApp()
	go func() {
		errCh <- app.RunWithContext(appCtx, cfg)
	}()

	select {
	case err := <-errCh:
		t.Fatalf("app stopped before publish: %v", err)
	case <-time.After(2 * time.Second):
	}

	pulsarClient, err := pulsar.NewClient(pulsar.ClientOptions{URL: cfg.Pulsar.URL})
	if err != nil {
		t.Fatalf("new pulsar client: %v", err)
	}
	defer pulsarClient.Close()

	producer, err := pulsarClient.CreateProducer(pulsar.ProducerOptions{Topic: topic})
	if err != nil {
		t.Fatalf("create producer: %v", err)
	}
	defer producer.Close()

	event := events.RewardRedemptionEvent{
		OperationID:  operationID,
		UserID:       userID,
		RewardID:     "integration-reward",
		UserRewardID: "integration-user-reward",
		OccurredAt:   "2026-05-19T10:00:00Z",
	}
	payload, err := json.Marshal(event)
	if err != nil {
		t.Fatalf("marshal event: %v", err)
	}

	if _, err := producer.Send(ctx, &pulsar.ProducerMessage{
		Key:     userID,
		Payload: payload,
		Properties: map[string]string{
			"eventType":   events.RewardRedeemedEventType,
			"operationId": operationID,
		},
	}); err != nil {
		t.Fatalf("send message: %v", err)
	}

	var stored map[string]string
	deadline := time.Now().Add(20 * time.Second)
	for time.Now().Before(deadline) {
		stored, err = redisClient.HGetAll(ctx, key).Result()
		if err != nil {
			t.Fatalf("hgetall: %v", err)
		}
		if stored["hash"] != "" && stored["json"] == string(payload) {
			break
		}
		time.Sleep(250 * time.Millisecond)
	}

	if stored["hash"] == "" {
		t.Fatalf("redis hash field is empty; stored=%v", stored)
	}
	if stored["json"] != string(payload) {
		t.Fatalf("redis json field = %q, want %q", stored["json"], string(payload))
	}
	if stored["operationId"] != operationID {
		t.Fatalf("redis operationId field = %q, want %q", stored["operationId"], operationID)
	}
}

func envOrDefault(key string, fallback string) string {
	value := os.Getenv(key)
	if value == "" {
		return fallback
	}

	return value
}
