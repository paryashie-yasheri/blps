package soaringLizards.blps.repository

import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import soaringLizards.blps.dto.userBalances.UserBalanceDto
import soaringLizards.blps.jooq.generated.tables.references.USER_BALANCES
import java.util.*

@Repository
data class UserBalanceRepository(
    private val dsl: DSLContext,
) {
    fun findById(id: UUID) =
        dsl.selectFrom(USER_BALANCES)
            .where(USER_BALANCES.BALANCE_ID.eq(id))
            .fetchOne()

    fun findByUserId(id: UUID) =
        dsl.selectFrom(USER_BALANCES)
            .where(USER_BALANCES.USER_ID.eq(id))
            .fetchOne()

    fun insert(balanceDto: UserBalanceDto): Int =
        dsl.insertInto(USER_BALANCES)
            .set(USER_BALANCES.USER_ID, balanceDto.userId)
            .set(USER_BALANCES.CURRENT_BALANCE, balanceDto.currentBalance)
            .set(USER_BALANCES.TOTAL_EARNED, balanceDto.totalEarned)
            .set(USER_BALANCES.TOTAL_SPENT, balanceDto.totalSpent)
            .set(USER_BALANCES.VERSION, balanceDto.version)
            .execute()

    fun update(balanceId: UUID, newBalance: Int, newTotalEarned: Int, newTotalSpent: Int, newVersion: Int): Int =
        dsl.update(USER_BALANCES)
            .set(USER_BALANCES.CURRENT_BALANCE, newBalance)
            .set(USER_BALANCES.TOTAL_EARNED, newTotalEarned)
            .set(USER_BALANCES.TOTAL_SPENT, newTotalSpent)
            .set(USER_BALANCES.VERSION, newVersion)
            .where(USER_BALANCES.BALANCE_ID.eq(balanceId))
            .execute()


    fun delete(id: UUID) =
        dsl.deleteFrom(USER_BALANCES)
            .where(USER_BALANCES.BALANCE_ID.eq(id))
            .execute()

    fun deleteByUserId(id: UUID) =
        dsl.deleteFrom(USER_BALANCES)
            .where(USER_BALANCES.USER_ID.eq(id))
            .execute()
}
