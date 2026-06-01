package events

import "testing"

func TestDecodeRewardRedemptionMessage(t *testing.T) {
	payload := []byte(`{"operationId":"op-1","userId":"user-1","rewardId":"reward-1","userRewardId":"user-reward-1","occurredAt":"2026-05-19T10:00:00Z"}`)

	message, err := DecodeRewardRedemptionMessage(payload, "user-1", map[string]string{
		"eventType":   RewardRedeemedEventType,
		"operationId": "op-1",
	})
	if err != nil {
		t.Fatalf("DecodeRewardRedemptionMessage() error = %v", err)
	}

	if message.Event.OperationID != "op-1" {
		t.Fatalf("operationId = %q, want %q", message.Event.OperationID, "op-1")
	}
	if message.RawJSON != string(payload) {
		t.Fatalf("RawJSON = %q, want %q", message.RawJSON, string(payload))
	}
	if message.EventType != RewardRedeemedEventType {
		t.Fatalf("EventType = %q, want %q", message.EventType, RewardRedeemedEventType)
	}
}

func TestDecodeRewardRedemptionMessageInvalidJSON(t *testing.T) {
	_, err := DecodeRewardRedemptionMessage([]byte(`{`), "", nil)
	if err == nil {
		t.Fatal("DecodeRewardRedemptionMessage() error = nil, want error")
	}
}
