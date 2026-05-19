package soaringlizards.datalakesync.spark.iceberg

import jakarta.annotation.PostConstruct
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service
import soaringlizards.datalakesync.dto.WriteRequest
import soaringlizards.datalakesync.spark.SparkSqlResult
import java.util.*

@Service
@Profile("!test")
class LedgerDataLakeService(
    private val tableManager: LedgerTableManager,
    private val ledgerDataLakeRepository: LedgerDataLakeRepository,
) {
    @PostConstruct
    fun initialize() {
        tableManager.createNamespace()
        tableManager.createTable()
    }

    fun insert(request: WriteRequest) =
        ledgerDataLakeRepository.insert(
            eventId = request.eventId ?: UUID.randomUUID().toString(),
            operationId = request.operationId,
            eventType = request.eventType,
            occurredAt = request.occurredAt,
            userId = request.userId,
            balanceId = request.balanceId,
            amount = request.amount,
            balanceBefore = request.balanceBefore,
            balanceAfter = request.balanceAfter,
            rewardId = request.rewardId,
            description = request.description,
        )

    fun read(limit: Int): SparkSqlResult {
        return ledgerDataLakeRepository.selectAll(limit)
    }

    fun readByUser(
        userId: String,
        limit: Int,
    ): SparkSqlResult {
        return ledgerDataLakeRepository.selectByUserId(userId, limit)
    }

    fun count(): SparkSqlResult {
        return ledgerDataLakeRepository.count()
    }

    fun seed(): SparkSqlResult {
        val results = mutableListOf<SparkSqlResult>()
        for (i in 1..5) {
            val eventId = UUID.randomUUID().toString()
            val operationId = UUID.randomUUID().toString()
            val userId = UUID.randomUUID().toString()
            val balanceId = UUID.randomUUID().toString()

            val result = ledgerDataLakeRepository.insert(
                eventId = eventId,
                operationId = operationId,
                eventType = "DEPOSIT",
                occurredAt = "2026-05-12T10:00:00.000Z",
                userId = userId,
                balanceId = balanceId,
                amount = i * 100,
                balanceBefore = (i - 1) * 100,
                balanceAfter = i * 100,
                rewardId = null,
                description = "Test deposit $i",
            )
            results.add(result)
        }
        return results.last()
    }
}
