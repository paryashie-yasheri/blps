package soaringlizards.datalakesync.spark.iceberg

import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Repository
import soaringlizards.datalakesync.config.SparkSqlConfig
import soaringlizards.datalakesync.spark.QueryParam
import soaringlizards.datalakesync.spark.SparkSqlExecutor
import soaringlizards.datalakesync.spark.SparkSqlResult
import soaringlizards.datalakesync.spark.query
import soaringlizards.datalakesync.sync.LedgerEventRow

@Repository
@Profile("!test")
class LedgerDataLakeRepository(
    config: SparkSqlConfig,
) {
    private val executorBuilder = SparkSqlExecutor.builder(config)

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

    fun insertBatch(events: List<LedgerEventRow>): SparkSqlResult {
        if (events.isEmpty()) return SparkSqlResult("", "", 0)
        val rowSql = "(?, ?, ?, CAST(? AS TIMESTAMP), ?, ?, ?, ?, ?, ?, ?)"
        val valuesSql = events.joinToString(", ") { rowSql }
        val allParams = events.flatMap { e ->
            val ts = e.occurredAt.toString().replace("T", " ").replace("+00:00", "")
            listOf(
                QueryParam.str(e.eventId),
                QueryParam.str(e.operationId),
                QueryParam.str(e.eventType),
                QueryParam.str(ts),
                QueryParam.str(e.userId),
                QueryParam.str(e.balanceId),
                QueryParam.int(e.amount),
                QueryParam.int(e.balanceBefore),
                QueryParam.int(e.balanceAfter),
                QueryParam.str(e.rewardId ?: ""),
                QueryParam.str(e.description ?: ""),
            )
        }
        return query(
            "INSERT INTO ${ns()}.${tbl()} VALUES $valuesSql",
            *allParams.toTypedArray(),
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

    fun selectByEventIds(eventIds: List<String>): SparkSqlResult {
        if (eventIds.isEmpty()) return SparkSqlResult("", "", 0)
        val inClause = eventIds.joinToString(",") { "'$it'" }
        return executorBuilder.execute(
            "SELECT event_id FROM ${ns()}.${tbl()} WHERE event_id IN ($inClause)",
        )
    }
}
