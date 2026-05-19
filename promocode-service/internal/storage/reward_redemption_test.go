package storage

import "testing"

func TestRewardRedemptionKey(t *testing.T) {
	got := RewardRedemptionKey("op-1")
	want := "promocode:reward-redemption:op-1"

	if got != want {
		t.Fatalf("RewardRedemptionKey() = %q, want %q", got, want)
	}
}
