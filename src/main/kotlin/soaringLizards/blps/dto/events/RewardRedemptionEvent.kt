package soaringLizards.blps.dto.events

import java.util.UUID

data class RewardRedemptionEvent(
    val operationId: UUID,
    val userId: UUID,
    val rewardId: UUID,
    val userRewardId: UUID,
    val occurredAt: String,
)
