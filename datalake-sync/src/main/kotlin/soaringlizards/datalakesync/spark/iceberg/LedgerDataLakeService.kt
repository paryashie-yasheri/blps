package soaringlizards.datalakesync.spark.iceberg

import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service
import soaringlizards.datalakesync.config.SparkSqlConfig
import soaringlizards.datalakesync.spark.QueryParam
import soaringlizards.datalakesync.spark.SparkSqlExecutor
import soaringlizards.datalakesync.spark.SparkSqlResult
import soaringlizards.datalakesync.spark.query

@Service
@Profile("!test")
class LedgerDataLakeService(
    private val tableManager: LedgerTableManager,
    config: SparkSqlConfig,
) {
    private val executorBuilder = SparkSqlExecutor.builder(config)

    @jakarta.annotation.PostConstruct
    fun initialize() {
        tableManager.createNamespace()
        tableManager.createTable()
    }

    private fun ns() = "datalake"
    private fun tbl() = "ledger_events"

    fun insert(
        eventId: String,
        operationId: String,
        eventType: String,
        occurredAt: String,
        userId: String,
        balanceId: String,
        amount: Int,
        balanceBefore: Int,
        balanceAfter: Int,
        rewardId: String?,
        description: String?,
    ): SparkSqlResult {
        return query(
            "INSERT INTO ${ns()}.${tbl()} VALUES (?, ?, ?, CAST(? AS TIMESTAMP), ?, ?, ?, ?, ?, ?, ?)",
            QueryParam.str(eventId),
            QueryParam.str(operationId),
            QueryParam.str(eventType),
            QueryParam.str(occurredAt),
            QueryParam.str(userId),
            QueryParam.str(balanceId),
            QueryParam.int(amount),
            QueryParam.int(balanceBefore),
            QueryParam.int(balanceAfter),
            QueryParam.str(rewardId ?: ""),
            QueryParam.str(description ?: ""),
        ).executeWith(executorBuilder)
    }

    fun selectAll(limit: Int = 100): SparkSqlResult {
        return query(
            "SELECT * FROM ${ns()}.${tbl()} LIMIT ?",
            QueryParam.int(limit),
        ).executeWith(executorBuilder)
    }

    fun selectByUserId(userId: String, limit: Int = 100): SparkSqlResult {
        return query(
            "SELECT * FROM ${ns()}.${tbl()} WHERE user_id = ? LIMIT ?",
            QueryParam.str(userId),
            QueryParam.int(limit),
        ).executeWith(executorBuilder)
    }

    fun selectByEventType(eventType: String, limit: Int = 100): SparkSqlResult {
        return query(
            "SELECT * FROM ${ns()}.${tbl()} WHERE event_type = ? LIMIT ?",
            QueryParam.str(eventType),
            QueryParam.int(limit),
        ).executeWith(executorBuilder)
    }

    fun count(): SparkSqlResult {
        return query("SELECT COUNT(*) FROM ${ns()}.${tbl()}").executeWith(executorBuilder)
    }
}
