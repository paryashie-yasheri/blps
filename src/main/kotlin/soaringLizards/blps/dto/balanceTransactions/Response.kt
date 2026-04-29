package soaringLizards.blps.dto.balanceTransactions

import soaringLizards.blps.entity.UserBalance
import soaringLizards.blps.jooq.generated.tables.records.BalanceTransactionsRecord
import soaringLizards.blps.jooq.generated.tables.records.UserBalancesRecord
import java.time.OffsetDateTime

class BalanceResponse(
    val balance: Int,
    val currentBalance: Int,
    val totalEarned: Int,
    val totalSpent: Int,
)

fun UserBalancesRecord.toBalanceResponse() = BalanceResponse(
    balance = currentBalance!!,
    currentBalance = currentBalance!!,
    totalEarned = totalEarned!!,
    totalSpent = totalSpent!!,
)

fun UserBalance.toBalanceResponse() = BalanceResponse(
    balance = currentBalance!!,
    currentBalance = currentBalance,
    totalEarned = totalEarned!!,
    totalSpent = totalSpent!!,
)

class TransactionsResponse(
    val transactionDifference: Int,
    val balanceBefore: Int,
    val balanceAfter: Int,
    val createdAt: OffsetDateTime,
)

fun BalanceTransactionsRecord.toTransactionResponse() = TransactionsResponse(
    amount!!,
    balanceBefore!!,
    balanceAfter!!,
    createdAt!!,
)
