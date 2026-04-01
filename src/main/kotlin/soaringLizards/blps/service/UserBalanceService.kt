package soaringLizards.blps.service

import org.springframework.security.access.AccessDeniedException
import org.springframework.security.core.userdetails.User
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import soaringLizards.blps.dto.balanceTransactions.*
import soaringLizards.blps.dto.userBalances.UserBalanceDto
import soaringLizards.blps.dto.userBalances.toUserBalanceDto
import soaringLizards.blps.exception.BusinessLogicException
import soaringLizards.blps.jooq.generated.tables.records.BalanceTransactionsRecord
import soaringLizards.blps.jooq.generated.tables.records.UserBalancesRecord
import soaringLizards.blps.jooq.generated.tables.references.USER_BALANCES
import soaringLizards.blps.repository.UserBalanceRepository
import java.time.OffsetDateTime
import java.util.*

@Service
class UserBalanceService(
    private val userBalanceRepository: UserBalanceRepository,
    private val userService: UserService,
    private val transactionService: TransactionService,
) {


    fun getCurrentUserBalance(currentUser: User?): BalanceResponse {
        currentUser ?: throw AccessDeniedException("Not logged in")

        val user = userService.convertSpringUserToUser(currentUser)

        val balance =
            findBalanceByUserId(user.id!!) ?: throw AccessDeniedException("Not created balance")

        return balance.toBalanceResponse()
    }

    fun donatePoints(currentUser: User?, request: DonateRequest): TransactionsResponse {
        currentUser ?: throw AccessDeniedException("Not logged in")

        if (request.amount <= 0) {
            throw BusinessLogicException("Cant donate zero or negative amount")
        }

        val user = userService.convertSpringUserToUser(currentUser)
        val balance =
            findBalanceByUserId(user.id!!) ?: throw AccessDeniedException("Not created balance")

        if (request.amount > balance.currentBalance!!) {
            throw BusinessLogicException("Unsufficient balance")
        }

        val balanceDiff = -request.amount
        val newTransaction = createTransactionOnUser(balance, balanceDiff).toTransactionResponse()
        val newBalance = balance.copy().with(USER_BALANCES.CURRENT_BALANCE, balance.currentBalance!! + balanceDiff)

        userBalanceRepository.update(
            balance.balanceId!!,
            newBalance.toUserBalanceDto(),
        )

        return newTransaction
    }

    @Transactional
    fun createTransactionOnUser(userBalance: UserBalancesRecord, balanceDiff: Int): BalanceTransactionsRecord {
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

        newTransaction ?: throw BusinessLogicException("Cannot add transaction")

        return newTransaction
    }

    fun findBalanceById(id: UUID) =
        userBalanceRepository.findById(id)

    fun findBalanceByUserId(id: UUID) =
        userBalanceRepository.findByUserId(id)

    fun addUserBalance(userBalanceDto: UserBalanceDto) =
        userBalanceRepository.insert(userBalanceDto)

    fun deleteUserBalance(userBalanceId: UUID) =
        userBalanceRepository.delete(userBalanceId)

    fun getBalanceByUserBalanceId(userBalanceId: UUID) =
        userBalanceRepository.findById(userBalanceId)

}
