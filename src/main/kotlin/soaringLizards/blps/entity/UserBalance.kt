package soaringLizards.blps.entity

import java.util.UUID

data class UserBalance(
    val balanceId: UUID?,
    val userId: UUID?,
    val currentBalance: Int?,
    val totalEarned: Int?,
    val totalSpent: Int?,
    val version: Int?,
)
