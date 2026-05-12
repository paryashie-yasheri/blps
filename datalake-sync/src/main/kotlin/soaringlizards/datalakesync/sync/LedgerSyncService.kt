package soaringlizards.datalakesync.sync

import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Service
import soaringlizards.datalakesync.config.LedgerSyncConfig
import soaringlizards.datalakesync.spark.PreparedQuery
import soaringlizards.datalakesync.spark.QueryParam
import soaringlizards.datalakesync.spark.SparkSqlExecutor
import soaringlizards.datalakesync.spark.query
import java.time.OffsetDateTime

@Service
@Profile("!test")
class LedgerSyncService(
    private val jdbcTemplate: JdbcTemplate,
    private val config: LedgerSyncConfig,
    configForSpark: soaringlizards.datalakesync.config.SparkSqlConfig,
) {
    private val logger = LoggerFactory.getLogger(LedgerSyncService::class.java)
    private val executorBuilder = SparkSqlExecutor.builder(configForSpark)
    private val ns = "datalake"
    private val tbl = "ledger_events"

    data class SyncResult(
        val fetched: Int,
        val inserted: Int,
        val cursor: OffsetDateTime?,
    )

    fun sync(): SyncResult {
        val cursor = getLastSyncedCursor()
        val newEvents = fetchNewEvents(cursor)
        if (newEvents.isEmpty()) {
            logger.info("No new ledger events to sync. Cursor={}", cursor)
            return SyncResult(0, 0, cursor)
        }
        logger.info("Fetched {} new ledger events. Cursor={}", newEvents.size, cursor)
        val inserted = insertBatch(newEvents)
        val lastOccurredAt = newEvents.maxOf { it.occurredAt }
        logger.info("Inserted {} events to Iceberg. Last occurred_at={}", inserted, lastOccurredAt)
        return SyncResult(newEvents.size, inserted, lastOccurredAt)
    }

    fun getLastSyncedCursor(): OffsetDateTime? {
        val result = query("SELECT MAX(occurred_at) FROM $ns.$tbl")
            .executeWith(executorBuilder)
        if (!result.isSuccess) {
            logger.warn("Failed to query MAX(occurred_at): {}", result.stdout)
            return null
        }
        val line = result.stdout.lines()
            .firstOrNull { line ->
                val trimmed = line.trim()
                trimmed.isNotBlank() &&
                    trimmed != "NULL" &&
                    !line.startsWith("Time taken") &&
                    !line.contains("(") &&
                    !line.startsWith("Setting ") &&
                    !line.startsWith("To adjust") &&
                    !line.startsWith("Spark ") &&
                    !line.startsWith("26/") &&
                    Regex("\\d{4}-\\d{2}-\\d{2}").containsMatchIn(line)
            }
            ?.trim()
        if (line == null) return null
        return try {
            OffsetDateTime.parse(line.replace(" ", "T") + "+00:00")
        } catch (e: Exception) {
            logger.warn("Failed to parse cursor '{}': {}", line, e.message)
            null
        }
    }

    fun fetchNewEvents(since: OffsetDateTime?): List<LedgerEventRow> {
        val sql = if (since == null) {
            "SELECT event_id, operation_id, event_type, occurred_at, user_id, balance_id, amount, balance_before, balance_after, reward_id, description FROM ledger_events ORDER BY occurred_at ASC"
        } else {
            "SELECT event_id, operation_id, event_type, occurred_at, user_id, balance_id, amount, balance_before, balance_after, reward_id, description FROM ledger_events WHERE occurred_at > ?::timestamptz ORDER BY occurred_at ASC"
        }

        val batchSize = config.sync.batchSize
        val rows = mutableListOf<LedgerEventRow>()

        if (since == null) {
            jdbcTemplate.query(sql) { rs -> rows.add(mapRow(rs)) }
        } else {
            val param = since.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSSxxxxx"))
            jdbcTemplate.query(sql, { ps -> ps.setString(1, param) }) { rs ->
                rows.add(mapRow(rs))
            }
        }

        return rows.take(batchSize)
    }

    private fun mapRow(rs: java.sql.ResultSet): LedgerEventRow {
        val rewardIdObj = rs.getObject("reward_id", java.util.UUID::class.java)
        return LedgerEventRow(
            eventId = rs.getObject("event_id", java.util.UUID::class.java)?.toString() ?: "",
            operationId = rs.getObject("operation_id", java.util.UUID::class.java).toString(),
            eventType = rs.getString("event_type"),
            occurredAt = rs.getObject("occurred_at", OffsetDateTime::class.java),
            userId = rs.getObject("user_id", java.util.UUID::class.java).toString(),
            balanceId = rs.getObject("balance_id", java.util.UUID::class.java).toString(),
            amount = rs.getInt("amount"),
            balanceBefore = rs.getInt("balance_before"),
            balanceAfter = rs.getInt("balance_after"),
            rewardId = rewardIdObj?.toString(),
            description = rs.getString("description"),
        )
    }

    fun insertBatch(events: List<LedgerEventRow>): Int {
        if (events.isEmpty()) return 0
        var inserted = 0
        val existingIds = getExistingEventIds(events.map { it.eventId })
        for (e in events) {
            if (e.eventId in existingIds) {
                logger.debug("Skipping duplicate event_id={}", e.eventId)
                continue
            }
            val ts = e.occurredAt.toString().replace("T", " ").replace("+00:00", "")
            val result = query(
                "INSERT INTO $ns.$tbl VALUES (?, ?, ?, CAST(? AS TIMESTAMP), ?, ?, ?, ?, ?, ?, ?)",
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
            ).executeWith(executorBuilder)
            if (result.isSuccess) inserted++ else logger.warn("Insert failed for event_id={}: {}", e.eventId, result.stdout)
        }
        return inserted
    }

    private fun getExistingEventIds(eventIds: List<String>): Set<String> {
        if (eventIds.isEmpty()) return emptySet()
        val inClause = eventIds.joinToString(",") { "'$it'" }
        val result = executorBuilder.execute(
            "SELECT event_id FROM $ns.$tbl WHERE event_id IN ($inClause)"
        )
        if (!result.isSuccess) return emptySet()
        return result.stdout.lines()
            .filter { it.isNotBlank() && !it.startsWith("Time taken") && !it.startsWith("event_id") }
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .toSet()
    }

    data class LedgerEventRow(
        val eventId: String,
        val operationId: String,
        val eventType: String,
        val occurredAt: OffsetDateTime,
        val userId: String,
        val balanceId: String,
        val amount: Int,
        val balanceBefore: Int,
        val balanceAfter: Int,
        val rewardId: String?,
        val description: String?,
    )
}
