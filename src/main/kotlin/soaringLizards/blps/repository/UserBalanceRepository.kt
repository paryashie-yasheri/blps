package soaringLizards.blps.repository

import org.jooq.DSLContext
import soaringLizards.blps.dto.userBalances.UserBalanceDto
import soaringLizards.blps.jooq.generated.tables.references.USER_BALANCES
import java.util.*

data class UserBalanceRepository(
    private val dsl: DSLContext,
) {
    fun findById(id: UUID) =
        dsl.select(USER_BALANCES)
            .where(USER_BALANCES.BALANCE_ID.eq(id))
            .fetchOne()

    fun insert(balanceDto: UserBalanceDto): Int =
        dsl.insertInto(USER_BALANCES)
            .set(USER_BALANCES.USER_ID, balanceDto.userId)
            .set(USER_BALANCES.CURRENT_BALANCE, balanceDto.currentBalance)
            .set(USER_BALANCES.TOTAL_EARNED, balanceDto.totalEarned)
            .set(USER_BALANCES.TOTAL_SPENT, balanceDto.totalSpent)
            .set(USER_BALANCES.VERSION, balanceDto.version)
            .execute()
}
