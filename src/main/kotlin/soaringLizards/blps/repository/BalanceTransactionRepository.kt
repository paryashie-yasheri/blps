package soaringLizards.blps.repository

import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import soaringLizards.blps.dto.balanceTransactions.BalanceTransactionDto
import soaringLizards.blps.jooq.generated.tables.references.BALANCE_TRANSACTIONS
import java.util.*

@Repository
class BalanceTransactionRepository(
    private val dsl: DSLContext,
) {
    fun findById(id: UUID) =
        dsl.select(BALANCE_TRANSACTIONS)
            .where(BALANCE_TRANSACTIONS.TRANSACTION_ID.eq(id))
            .fetchOne()

    fun insert(transactionDto: BalanceTransactionDto) =
        dsl.insertInto(BALANCE_TRANSACTIONS)
            .set(BALANCE_TRANSACTIONS.BALANCE_ID, transactionDto.balanceId)
            .set(BALANCE_TRANSACTIONS.TRANSACTION_TYPE, transactionDto.transactionType)
            .set(BALANCE_TRANSACTIONS.AMOUNT, transactionDto.amount)
            .set(BALANCE_TRANSACTIONS.BALANCE_BEFORE, transactionDto.balanceBefore)
            .set(BALANCE_TRANSACTIONS.BALANCE_AFTER, transactionDto.balanceAfter)
            .set(BALANCE_TRANSACTIONS.USER_ACHIEVEMENT_ID, transactionDto.userAchievementId)
            .set(BALANCE_TRANSACTIONS.ACHIEVEMENT_TYPE_ID, transactionDto.achievementTypeId)
            .set(BALANCE_TRANSACTIONS.DESCRIPTION, transactionDto.description)
            .set(BALANCE_TRANSACTIONS.CREATED_AT, transactionDto.createdAt)
            .execute()

    fun delete(id: UUID) =
        dsl.deleteFrom(BALANCE_TRANSACTIONS)
            .where(BALANCE_TRANSACTIONS.TRANSACTION_ID.eq(id))
            .execute()
}
