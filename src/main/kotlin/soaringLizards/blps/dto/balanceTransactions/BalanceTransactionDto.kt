package soaringLizards.blps.dto.balanceTransactions

import java.time.OffsetDateTime
import java.util.*

data class BalanceTransactionDto(
    val balanceId: UUID,
    val transactionType: String,
    val amount: Int,
    val balanceBefore: Int,
    val balanceAfter: Int,
    val userAchievementId: UUID?,
    val achievementTypeId: UUID?,
    val description: String?,
    val createdAt: OffsetDateTime,
)
