package handler

import (
	"context"
	"testing"

	"promocode-service/internal/storage"
)

type fakeStore struct {
	record storage.RewardRedemptionRecord
	calls  int
	err    error
}

func (s *fakeStore) SaveRewardRedemption(_ context.Context, record storage.RewardRedemptionRecord) error {
	s.calls++
	s.record = record
	return s.err
}

func TestHandleMessageSavesHashAndRawJSON(t *testing.T) {
	store := &fakeStore{}
	handler := New(store)

	payload := []byte(`{"operationId":"op-1","userId":"user-1","rewardId":"reward-1","userRewardId":"user-reward-1","occurredAt":"2026-05-19T10:00:00Z"}`)
	err := handler.HandleMessage(context.Background(), Message{
		ID:      "msg-1",
		Key:     "user-1",
		Payload: payload,
		Properties: map[string]string{
			"eventType":   "REWARD_REDEEMED",
			"operationId": "op-1",
		},
	})
	if err != nil {
		t.Fatalf("HandleMessage() error = %v", err)
	}

	if store.calls != 1 {
		t.Fatalf("store calls = %d, want 1", store.calls)
	}
	if store.record.Message.RawJSON != string(payload) {
		t.Fatalf("stored json = %q, want %q", store.record.Message.RawJSON, string(payload))
	}
	if store.record.Hash == "" {
		t.Fatal("stored hash is empty")
	}
}

func TestHandleMessageInvalidJSON(t *testing.T) {
	store := &fakeStore{}
	handler := New(store)

	err := handler.HandleMessage(context.Background(), Message{
		ID:      "msg-1",
		Payload: []byte(`{`),
	})
	if err == nil {
		t.Fatal("HandleMessage() error = nil, want error")
	}
	if store.calls != 0 {
		t.Fatalf("store calls = %d, want 0", store.calls)
	}
}
