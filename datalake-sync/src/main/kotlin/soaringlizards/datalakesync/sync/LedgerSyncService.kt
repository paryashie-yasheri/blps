package soaringlizards.datalakesync.sync

import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Service
import soaringlizards.datalakesync.config.LedgerSyncConfig
import soaringlizards.datalakesync.spark.SparkSqlExecutor
import soaringlizards.datalakesync.spark.iceberg.LedgerDataLakeRepository
import soaringlizards.datalakesync.spark.query
import java.time.OffsetDateTime

@Service
@Profile("!test")
class LedgerSyncService(
    private val jdbcTemplate: JdbcTemplate,
    private val config: LedgerSyncConfig,
    private val repository: LedgerDataLakeRepository,
    configForSpark: soaringlizards.datalakesync.config.SparkSqlConfig,
) {
    private val logger = LoggerFactory.getLogger(LedgerSyncService::class.java)
    private val executorBuilder = SparkSqlExecutor.builder(configForSpark)
    private val ns = "datalake"
    private val tbl = "ledger_events"

    companion object {
        private val TIMESTAMP_REGEX = Regex("\\d{4}-\\d{2}-\\d{2}")
    }

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
        val line = findTimestampLine(result.stdout.lines()) ?: return null
        return try {
            OffsetDateTime.parse(line.replace(" ", "T") + "+00:00")
        } catch (e: Exception) {
            logger.warn("Failed to parse cursor '{}': {}", line, e.message)
            null
        }
    }

    private fun findTimestampLine(lines: List<String>): String? {
        return lines.firstOrNull { line ->
            val trimmed = line.trim()
            trimmed.isNotBlank() &&
                    trimmed != "NULL" &&
                    !line.startsWith("Time taken") &&
                    !line.contains("(") &&
                    !line.startsWith("Setting ") &&
                    !line.startsWith("To adjust") &&
                    !line.startsWith("Spark ") &&
                    !line.startsWith("26/") &&
                    TIMESTAMP_REGEX.containsMatchIn(line)
        }?.trim()
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
        val eventIds = events.map { it.eventId }
        val existingResult = repository.selectByEventIds(eventIds)
        val existingIds = if (existingResult.isSuccess) {
            existingResult.stdout.lines()
                .filter { it.isNotBlank() && !it.startsWith("Time taken") && !it.startsWith("event_id") }
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .toSet()
        } else {
            emptySet()
        }
        val newEvents = events.filter { it.eventId !in existingIds }
        if (newEvents.isEmpty()) return 0
        val result = repository.insertBatch(newEvents)
        return if (result.isSuccess) newEvents.size else 0
    }
}
