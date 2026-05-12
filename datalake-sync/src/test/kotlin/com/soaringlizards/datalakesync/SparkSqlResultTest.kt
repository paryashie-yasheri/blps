package com.soaringlizards.datalakesync

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import com.soaringlizards.datalakesync.spark.SparkSqlResult
import org.junit.jupiter.api.Assertions.assertTrue

class SparkSqlResultTest {

    @Test
    fun `isSuccess returns true for exitCode 0`() {
        val result = SparkSqlResult("OK", "", 0)
        assertTrue(result.isSuccess)
    }

    @Test
    fun `isSuccess returns false for exitCode 1`() {
        val result = SparkSqlResult("ERROR", "", 1)
        assertTrue(!result.isSuccess)
    }

    @Test
    fun `isSuccess returns false for exitCode -1`() {
        val result = SparkSqlResult("TIMEOUT", "", -1)
        assertTrue(!result.isSuccess)
    }

    @Test
    fun `result stores stdout and stderr`() {
        val result = SparkSqlResult("stdout text", "stderr text", 0)
        assertEquals("stdout text", result.stdout)
        assertEquals("stderr text", result.stderr)
        assertEquals(0, result.exitCode)
    }
}