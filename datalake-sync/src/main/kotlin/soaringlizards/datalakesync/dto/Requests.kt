package soaringlizards.datalakesync.dto

data class WriteRequest(
    val eventId: String? = null,
    val operationId: String,
    val eventType: String,
    val occurredAt: String,
    val userId: String,
    val balanceId: String,
    val amount: Int,
    val balanceBefore: Int,
    val balanceAfter: Int,
    val rewardId: String? = null,
    val description: String? = null,
)
