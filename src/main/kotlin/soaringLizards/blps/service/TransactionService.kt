package soaringLizards.blps.service

import org.springframework.stereotype.Service
import soaringLizards.blps.dto.balanceTransactions.BalanceTransactionDto
import soaringLizards.blps.repository.BalanceTransactionRepository
import java.util.*

@Service
class TransactionService(
    private val transactionRepository: BalanceTransactionRepository,
) {

    fun findTransactionById(id: UUID) =
        transactionRepository.findById(id)

    fun addTransaction(transactionDto: BalanceTransactionDto) =
        transactionRepository.insert(transactionDto)

    fun deleteTransaction(transactionId: UUID) =
        transactionRepository.delete(transactionId)

}
