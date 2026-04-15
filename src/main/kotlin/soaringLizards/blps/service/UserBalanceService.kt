package soaringLizards.blps.service

import org.springframework.security.access.AccessDeniedException
import org.springframework.security.core.userdetails.User
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import soaringLizards.blps.dto.balanceTransactions.*
import soaringLizards.blps.dto.userBalances.UserBalanceDto
import soaringLizards.blps.exception.BusinessLogicException
import soaringLizards.blps.jooq.generated.tables.records.BalanceTransactionsRecord
import soaringLizards.blps.jooq.generated.tables.records.UserBalancesRecord
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

        val balance = userBalanceRepository.findByUserId(user.id!!)
            ?: throw AccessDeniedException("Not created balance")

        return balance.toBalanceResponse()
    }

    @Transactional
    fun donatePoints(currentUser: User?, request: DonateRequest): TransactionsResponse {
        currentUser ?: throw AccessDeniedException("Not logged in")

        if (request.amount <= 0)
            throw BusinessLogicException("Cant donate zero or negative amount")

        val user = userService.convertSpringUserToUser(currentUser)
        val balance = userBalanceRepository.findByUserId(user.id!!)
            ?: throw AccessDeniedException("Not created balance")

        if (request.amount > balance.currentBalance!!)
            throw BusinessLogicException("Unsufficient balance")

        val balanceDiff = -request.amount
        val newTransaction = createTransactionOnUser(balance, balanceDiff).toTransactionResponse()
        
        val newCurrentBalance = balance.currentBalance!! + balanceDiff
        val newTotalSpent = balance.totalSpent!! + request.amount

        userBalanceRepository.update(
            balance.balanceId!!,
            newCurrentBalance,
            balance.totalEarned!!,
            newTotalSpent,
            balance.version!! + 1,
        )

        return newTransaction
    }

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

    fun getBalanceByUsername(username: String): Int {
        val user = userService.getUserByUsername(username)
        println("DEBUG getBalanceByUsername: username=$username, user=$user")
        if (user == null) {
            throw AccessDeniedException("User not found: $username")
        }
        println("DEBUG: user.id=${user.id}")
        val balance = userBalanceRepository.findByUserId(user.id!!)
        println("DEBUG: balance=$balance")
        if (balance == null) {
            throw AccessDeniedException("No balance found for user: $username")
        }
        return balance.currentBalance!!
    }

}
