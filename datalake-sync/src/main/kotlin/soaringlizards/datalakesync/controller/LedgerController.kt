package soaringlizards.datalakesync.controller

import org.springframework.context.annotation.Profile
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import soaringlizards.datalakesync.spark.SparkSqlResult
import soaringlizards.datalakesync.spark.iceberg.LedgerDataLakeService
import java.util.UUID

@RestController
@RequestMapping("/ledger")
@Profile("dev", "prod")
class LedgerController(private val ledgerDataLakeService: LedgerDataLakeService) {

    @PostMapping("/write")
    fun write(@RequestBody request: WriteRequest): SparkSqlResult {
        return ledgerDataLakeService.insert(
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
    }

    @GetMapping("/read")
    fun read(@RequestParam(defaultValue = "100") limit: Int): SparkSqlResult {
        return ledgerDataLakeService.selectAll(limit)
    }

    @GetMapping("/read/user")
    fun readByUser(
        @RequestParam userId: String,
        @RequestParam(defaultValue = "100") limit: Int,
    ): SparkSqlResult {
        return ledgerDataLakeService.selectByUserId(userId, limit)
    }

    @GetMapping("/count")
    fun count(): SparkSqlResult {
        return ledgerDataLakeService.count()
    }

    @PostMapping("/seed")
    fun seed(): SparkSqlResult {
        val results = mutableListOf<SparkSqlResult>()
        for (i in 1..5) {
            val eventId = UUID.randomUUID().toString()
            val operationId = UUID.randomUUID().toString()
            val userId = UUID.randomUUID().toString()
            val balanceId = UUID.randomUUID().toString()
            val result = ledgerDataLakeService.insert(
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

    data class WriteRequest(
        val eventId: String? = null,
        val operationId: String,
        val eventType: String,
        val occurredAt: String,
        val userId: String,
        val balanceId: String,
        val amount: Int,
        val balanceBefore: Int,
        val balanceAfter: Int,
        val rewardId: String? = null,
        val description: String? = null,
    )
}