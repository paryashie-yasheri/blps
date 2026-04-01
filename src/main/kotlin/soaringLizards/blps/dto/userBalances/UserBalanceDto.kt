package soaringLizards.blps.dto.userBalances

import java.util.*

data class UserBalanceDto(
    val userId: UUID,
    val currentBalance: Int,
    val totalEarned: Int,
    val totalSpent: Int,
    val version: Int,
)
