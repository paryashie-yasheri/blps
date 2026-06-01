package handler

import (
	"context"
	"log"

	"github.com/apache/pulsar-client-go/pulsar"

	"promocode-service/internal/events"
	eventhash "promocode-service/internal/hash"
	"promocode-service/internal/storage"
)

type Message struct {
	ID         string
	Key        string
	Properties map[string]string
	Payload    []byte
}

type Handler struct {
	store storage.RewardRedemptionStore
}

func New(store storage.RewardRedemptionStore) *Handler {
	return &Handler{store: store}
}

func (h *Handler) HandleMessage(ctx context.Context, msg Message) error {
	redemption, err := events.DecodeRewardRedemptionMessage(msg.Payload, msg.Key, msg.Properties)
	if err != nil {
		return err
	}

	hash, err := eventhash.RewardRedemptionSHA256(redemption.Event)
	if err != nil {
		return err
	}

	if err := h.store.SaveRewardRedemption(ctx, storage.RewardRedemptionRecord{
		Message: redemption,
		Hash:    hash,
	}); err != nil {
		return err
	}

	log.Printf(
		"reward_redemption_saved msgID=%s operationId=%s userId=%s rewardId=%s hash=%s",
		msg.ID,
		redemption.EffectiveOperationID(),
		redemption.Event.UserID,
		redemption.Event.RewardID,
		hash,
	)

	return nil
}

func MessageFromPulsar(msg pulsar.Message) Message {
	return Message{
		ID:         msg.ID().String(),
		Key:        msg.Key(),
		Properties: msg.Properties(),
		Payload:    msg.Payload(),
	}
}
