package soaringLizards.blps.dto.userBalances

import soaringLizards.blps.jooq.generated.tables.records.UserBalancesRecord
import java.util.*

data class UserBalanceDto(
    val userId: UUID,
    val currentBalance: Int,
    val totalEarned: Int,
    val totalSpent: Int,
    val version: Int,
) {
    companion object {
        fun createDefault(userId: UUID) = UserBalanceDto(
            userId,
            1000,
            1000,
            0,
            1,
        )
    }
}

fun UserBalancesRecord.toUserBalanceDto() = UserBalanceDto(
    userId!!,
    currentBalance!!,
    totalEarned!!,
    totalSpent!!,
    version!!,
)
