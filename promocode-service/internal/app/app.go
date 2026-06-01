package app

import (
	"context"
	"log"
	"os"
	"os/signal"
	"sync"
	"syscall"
	"time"

	"github.com/apache/pulsar-client-go/pulsar"

	"promocode-service/internal/config"
	"promocode-service/internal/handler"
	"promocode-service/internal/storage"
)

func Run() error {
	ctx, stop := signal.NotifyContext(context.Background(), os.Interrupt, syscall.SIGTERM)
	defer stop()

	return RunWithContext(ctx, config.Load())
}

func RunWithContext(ctx context.Context, cfg config.Config) error {
	store := storage.NewRedisRewardRedemptionStore(cfg.Redis)
	defer func() {
		if err := store.Close(); err != nil {
			log.Printf("redis_close_failed error=%v", err)
		}
	}()

	if err := store.Ping(ctx); err != nil {
		return err
	}

	client, err := pulsar.NewClient(pulsar.ClientOptions{
		URL: cfg.Pulsar.URL,
	})
	if err != nil {
		return err
	}
	defer client.Close()

	consumer, err := client.Subscribe(pulsar.ConsumerOptions{
		Topic:               cfg.Pulsar.Topic,
		SubscriptionName:    cfg.Pulsar.Subscription,
		Type:                pulsar.Shared,
		ReceiverQueueSize:   cfg.Workers * 2,
		NackRedeliveryDelay: 30 * time.Second,
	})
	if err != nil {
		return err
	}
	defer consumer.Close()

	messageHandler := handler.New(store)
	jobs := make(chan pulsar.Message, cfg.Workers*2)

	var wg sync.WaitGroup
	for i := 0; i < cfg.Workers; i++ {
		wg.Add(1)

		go func(workerID int) {
			defer wg.Done()

			for msg := range jobs {
				if err := messageHandler.HandleMessage(ctx, handler.MessageFromPulsar(msg)); err != nil {
					log.Printf("worker=%d msgID=%v failed: %v", workerID, msg.ID(), err)
					consumer.Nack(msg)
					continue
				}

				if err := consumer.Ack(msg); err != nil {
					log.Printf("worker=%d msgID=%v ack_failed: %v", workerID, msg.ID(), err)
				}
			}
		}(i)
	}

receiveLoop:
	for {
		msg, err := consumer.Receive(ctx)
		if err != nil {
			if ctx.Err() != nil {
				break receiveLoop
			}

			log.Printf("pulsar_receive_failed error=%v", err)
			continue
		}

		select {
		case jobs <- msg:
		case <-ctx.Done():
			consumer.Nack(msg)
			break receiveLoop
		}
	}

	close(jobs)
	wg.Wait()

	log.Println("consumer stopped")
	return nil
}
