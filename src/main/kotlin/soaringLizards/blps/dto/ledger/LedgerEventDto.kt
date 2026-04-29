package soaringLizards.blps.dto.ledger

import java.time.OffsetDateTime
import java.util.UUID

data class LedgerEventDto(
    val operationId: UUID,
    val eventType: String,
    val occurredAt: OffsetDateTime,
    val userId: UUID,
    val balanceId: UUID,
    val amount: Int,
    val balanceBefore: Int,
    val balanceAfter: Int,
    val rewardId: UUID? = null,
    val description: String? = null,
)
