package main

import (
	"context"
	"fmt"
	"log"
	"os"
	"os/signal"
	"sync"
	"syscall"
	"time"

	"github.com/apache/pulsar-client-go/pulsar"
)

const (
	pulsarURL        = "pulsar://localhost:6650"
	topic            = "persistent://public/default/spring-requests"
	subscriptionName = "go-worker"
	workers          = 8
)

func main() {
	ctx, stop := signal.NotifyContext(context.Background(), os.Interrupt, syscall.SIGTERM)
	defer stop()

	client, err := pulsar.NewClient(pulsar.ClientOptions{
		URL: pulsarURL,
	})
	if err != nil {
		log.Fatal(err)
	}
	defer client.Close()

	consumer, err := client.Subscribe(pulsar.ConsumerOptions{
		Topic:               topic,
		SubscriptionName:    subscriptionName,
		Type:                pulsar.Shared,
		ReceiverQueueSize:   workers * 2,
		NackRedeliveryDelay: 30 * time.Second,
	})
	if err != nil {
		log.Fatal(err)
	}
	defer consumer.Close()

	jobs := make(chan pulsar.Message, workers*2)

	var wg sync.WaitGroup
	for i := 0; i < workers; i++ {
		wg.Add(1)

		go func(workerID int) {
			defer wg.Done()

			for msg := range jobs {
				if err := handleMessage(ctx, msg); err != nil {
					log.Printf(
						"worker=%d msgID=%v failed: %v",
						workerID,
						msg.ID(),
						err,
					)

					consumer.Nack(msg)
					continue
				}

				if err := consumer.Ack(msg); err != nil {
					log.Printf(
						"worker=%d msgID=%v ack failed: %v",
						workerID,
						msg.ID(),
						err,
					)
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

			log.Printf("receive error: %v", err)
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
}

func handleMessage(ctx context.Context, msg pulsar.Message) error {
	payload := msg.Payload()

	fmt.Printf("received msgID=%v payload=%s\n", msg.ID(), string(payload))

	// тут твоя бизнес-логика:
	// - распарсить JSON
	// - сходить в БД
	// - дернуть внешний API
	// - выполнить задачу
	// - отправить результат в другой topic, если нужен ответ

	return nil
}
