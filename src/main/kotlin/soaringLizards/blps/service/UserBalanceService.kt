package soaringLizards.blps.service

import org.springframework.security.access.AccessDeniedException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import jakarta.transaction.TransactionManager
import org.slf4j.LoggerFactory
import soaringLizards.blps.config.userDetails.JwtUserDetails
import soaringLizards.blps.dto.balanceTransactions.*
import soaringLizards.blps.dto.ledger.LedgerEventDto
import soaringLizards.blps.dto.userBalances.UserBalanceDto
import soaringLizards.blps.entity.UserBalance
import soaringLizards.blps.exception.BusinessLogicException
import soaringLizards.blps.jooq.generated.tables.records.BalanceTransactionsRecord
import soaringLizards.blps.repository.UserBalanceRepository
import java.time.OffsetDateTime
import java.util.*

@Service
class UserBalanceService(
    private val userBalanceRepository: UserBalanceRepository,
    private val transactionService: TransactionService,
    private val ledgerEventService: LedgerEventService,
    private val transactionManager: TransactionManager,
) {
    private val logger = LoggerFactory.getLogger(javaClass)


    fun getCurrentUserBalance(userId: UUID): BalanceResponse {
        val balance = userBalanceRepository.findByUserId(userId)
            ?: throw AccessDeniedException("Not created balance")

        return balance.toBalanceResponse()
    }

    @Transactional
    fun donatePoints(currentUser: JwtUserDetails?, request: DonateRequest): TransactionsResponse {
        currentUser ?: throw AccessDeniedException("Not logged in")
        val operationId = UUID.randomUUID()
        val xid = transactionManager.transaction?.toString()
        logger.info("donate_points_started operationId={} xid={} userId={} amount={}", operationId, xid, currentUser.id, request.amount)
        try {
            if (request.amount <= 0)
                throw BusinessLogicException("Cant donate zero or negative amount")

            val balance = userBalanceRepository.findByUserId(currentUser.id)
                ?: throw AccessDeniedException("Not created balance")

            if (request.amount > balance.currentBalance!!)
                throw BusinessLogicException("Unsufficient balance")

            val balanceDiff = -request.amount
            val newTransaction = createTransactionOnUser(balance, balanceDiff).toTransactionResponse()

            val newCurrentBalance = balance.currentBalance + balanceDiff
            val newTotalSpent = balance.totalSpent!! + request.amount

            userBalanceRepository.update(
                balance.balanceId!!,
                newCurrentBalance,
                balance.totalEarned!!,
                newTotalSpent,
                balance.version!! + 1,
            )
            ledgerEventService.append(
                LedgerEventDto(
                    operationId = operationId,
                    eventType = "DONATION",
                    occurredAt = OffsetDateTime.now(),
                    userId = currentUser.id,
                    balanceId = balance.balanceId,
                    amount = balanceDiff,
                    balanceBefore = balance.currentBalance,
                    balanceAfter = newCurrentBalance,
                    description = "Voluntary donation for testing purposes",
                ),
            )

            return newTransaction
        } catch (ex: Exception) {
            logger.error(
                "donate_points_failed operationId={} xid={} userId={} amount={}",
                operationId,
                xid,
                currentUser.id,
                request.amount,
                ex,
            )
            throw ex
        }
    }

    fun createTransactionOnUser(userBalance: UserBalance, balanceDiff: Int): BalanceTransactionsRecord {
        val currentBalance = userBalance.currentBalance!!

        val transaction = BalanceTransactionDto(
            userBalance.balanceId!!,
            "DONATION",
            balanceDiff,
            currentBalance,
            currentBalance + balanceDiff,
            null,
            null,
            "Voluntary donation for testing purposes",
            OffsetDateTime.now(),
        )

        val newTransaction = transactionService.addTransaction(transaction)
            ?: throw BusinessLogicException("Cannot add transaction")

        return newTransaction
    }

    fun findBalanceById(id: UUID) =
        userBalanceRepository.findById(id)

    fun addUserBalance(userBalanceDto: UserBalanceDto) =
        userBalanceRepository.insert(userBalanceDto)

    fun deleteUserBalance(userBalanceId: UUID) =
        userBalanceRepository.delete(userBalanceId)

    fun getBalanceByUserBalanceId(userBalanceId: UUID) =
        userBalanceRepository.findById(userBalanceId)

}
