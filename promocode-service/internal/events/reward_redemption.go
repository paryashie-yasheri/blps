package events

import (
	"encoding/json"
	"fmt"
)

const RewardRedeemedEventType = "REWARD_REDEEMED"

type RewardRedemptionEvent struct {
	OperationID  string `json:"operationId"`
	UserID       string `json:"userId"`
	RewardID     string `json:"rewardId"`
	UserRewardID string `json:"userRewardId"`
	OccurredAt   string `json:"occurredAt"`
}

type RewardRedemptionMessage struct {
	Key         string
	EventType   string
	OperationID string
	RawJSON     string
	Event       RewardRedemptionEvent
}

func DecodeRewardRedemptionMessage(payload []byte, key string, properties map[string]string) (RewardRedemptionMessage, error) {
	var event RewardRedemptionEvent
	if err := json.Unmarshal(payload, &event); err != nil {
		return RewardRedemptionMessage{}, fmt.Errorf("decode reward redemption event: %w", err)
	}

	return RewardRedemptionMessage{
		Key:         key,
		EventType:   properties["eventType"],
		OperationID: properties["operationId"],
		RawJSON:     string(payload),
		Event:       event,
	}, nil
}

func (m RewardRedemptionMessage) EffectiveOperationID() string {
	if m.Event.OperationID != "" {
		return m.Event.OperationID
	}

	return m.OperationID
}
