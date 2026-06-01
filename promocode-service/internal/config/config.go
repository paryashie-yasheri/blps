package config

import (
	"os"
	"strconv"
)

const (
	defaultPulsarURL          = "pulsar://localhost:6650"
	defaultPulsarTopic        = "persistent://public/default/reward-redemptions"
	defaultPulsarSubscription = "promocode-service"
	defaultRedisAddr          = "localhost:6380"
	defaultRedisPassword      = ""
	defaultRedisDB            = 0
	defaultWorkers            = 8
)

type Config struct {
	Pulsar  PulsarConfig
	Redis   RedisConfig
	Workers int
}

type RedisConfig struct {
	Addr     string
	Password string
	DB       int
}

type PulsarConfig struct {
	URL          string
	Topic        string
	Subscription string
}

func Load() Config {
	return Config{
		Pulsar: PulsarConfig{
			URL:          envOrDefault("PULSAR_URL", defaultPulsarURL),
			Topic:        envOrDefault("PULSAR_TOPIC", defaultPulsarTopic),
			Subscription: envOrDefault("PULSAR_SUBSCRIPTION", defaultPulsarSubscription),
		},
		Redis: RedisConfig{
			Addr:     envOrDefault("REDIS_ADDR", defaultRedisAddr),
			Password: envOrDefault("REDIS_PASSWORD", defaultRedisPassword),
			DB:       envIntOrDefault("REDIS_DB", defaultRedisDB),
		},
		Workers: envIntOrDefault("WORKERS", defaultWorkers),
	}
}

func envOrDefault(key string, fallback string) string {
	value := os.Getenv(key)
	if value == "" {
		return fallback
	}

	return value
}

func envIntOrDefault(key string, fallback int) int {
	value := os.Getenv(key)
	if value == "" {
		return fallback
	}

	parsed, err := strconv.Atoi(value)
	if err != nil {
		return fallback
	}

	return parsed
}
