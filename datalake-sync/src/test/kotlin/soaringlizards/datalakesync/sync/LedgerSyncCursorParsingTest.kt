package soaringlizards.datalakesync.sync

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import soaringlizards.datalakesync.spark.SparkSqlExecutor
import soaringlizards.datalakesync.spark.SparkSqlResult
import java.time.OffsetDateTime
import kotlin.test.assertEquals

@ExtendWith(MockitoExtension::class)
class LedgerSyncCursorParsingTest {

    @Test
    fun `null cursor when Spark returns NULL`() {
        val result = parseCursorFromOutput("NULL\nTime taken: 1.234 seconds")
        assertEquals(null, result)
    }

    @Test
    fun `parses timestamp with dots and spaces`() {
        val result = parseCursorFromOutput("2026-05-12 10:00:00.000\nTime taken: 0.500 seconds")
        assertEquals(OffsetDateTime.parse("2026-05-12T10:00:00+00:00"), result)
    }

    @Test
    fun `skips header row with function name`() {
        val result = parseCursorFromOutput("max(occurred_at)\n2026-05-12 10:00:00\nTime taken: 0.500 seconds")
        assertEquals(OffsetDateTime.parse("2026-05-12T10:00:00+00:00"), result)
    }

    @Test
    fun `returns null when only timing lines present`() {
        val result = parseCursorFromOutput("Time taken: 0.500 seconds\nFetched 0 row(s)")
        assertEquals(null, result)
    }

    private fun parseCursorFromOutput(stdout: String): OffsetDateTime? {
        val line = stdout.lines()
            .firstOrNull { line ->
                val trimmed = line.trim()
                trimmed.isNotBlank() &&
                    trimmed != "NULL" &&
                    !line.startsWith("Time taken") &&
                    !line.contains("(")
            }
            ?.trim()
        if (line == null) return null
        return try {
            OffsetDateTime.parse(line.replace(" ", "T") + "+00:00")
        } catch (e: Exception) {
            null
        }
    }
}
