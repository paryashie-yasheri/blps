package hash

import (
	"crypto/sha256"
	"encoding/hex"
	"testing"

	"promocode-service/internal/events"
)

func TestRewardRedemptionSHA256(t *testing.T) {
	event := events.RewardRedemptionEvent{
		OperationID:  "op-1",
		UserID:       "user-1",
		RewardID:     "reward-1",
		UserRewardID: "user-reward-1",
		OccurredAt:   "2026-05-19T10:00:00Z",
	}

	got, err := RewardRedemptionSHA256(event)
	if err != nil {
		t.Fatalf("RewardRedemptionSHA256() error = %v", err)
	}

	canonicalJSON := `{"operationId":"op-1","userId":"user-1","rewardId":"reward-1","userRewardId":"user-reward-1","occurredAt":"2026-05-19T10:00:00Z"}`
	sum := sha256.Sum256([]byte(canonicalJSON))
	want := hex.EncodeToString(sum[:])

	if got != want {
		t.Fatalf("RewardRedemptionSHA256() = %q, want %q", got, want)
	}
}
