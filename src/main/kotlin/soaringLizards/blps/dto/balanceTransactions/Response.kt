package soaringLizards.blps.dto.balanceTransactions

import soaringLizards.blps.jooq.generated.tables.records.BalanceTransactionsRecord
import soaringLizards.blps.jooq.generated.tables.records.UserBalancesRecord
import java.time.OffsetDateTime

class BalanceResponse(
    val currentBalance: Int,
    val totalEarned: Int,
    val totalSpent: Int,
)

fun UserBalancesRecord.toBalanceResponse() = BalanceResponse(
    currentBalance!!,
    totalEarned!!,
    totalSpent!!,
)

class TransactionsResponse(
    val currentBalance: Int,
    val balanceBefore: Int,
    val createdAt: OffsetDateTime,
)

fun BalanceTransactionsRecord.toTransactionResponse() = TransactionsResponse(
    amount!!,
    balanceBefore!!,
    createdAt!!,
)
