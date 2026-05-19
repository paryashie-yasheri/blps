package soaringlizards.datalakesync.sync

import java.time.OffsetDateTime

data class LedgerEventRow(
    val eventId: String,
    val operationId: String,
    val eventType: String,
    val occurredAt: OffsetDateTime,
    val userId: String,
    val balanceId: String,
    val amount: Int,
    val balanceBefore: Int,
    val balanceAfter: Int,
    val rewardId: String?,
    val description: String?,
)
