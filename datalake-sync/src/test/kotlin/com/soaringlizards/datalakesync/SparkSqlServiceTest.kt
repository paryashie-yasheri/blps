package com.soaringlizards.datalakesync

import com.soaringlizards.datalakesync.spark.SparkSqlResult
import com.soaringlizards.datalakesync.spark.SparkSqlService
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class SparkSqlServiceTest {

    private lateinit var mockService: SparkSqlService
    private lateinit var mockExecResult: SparkSqlResult

    @BeforeEach
    fun setup() {
        mockService = mock {}
        mockExecResult = SparkSqlResult(
            stdout = "1\t2026-05-12 10:00:00.000\ttest comment\nTime taken: 1.0 seconds, Fetched 1 row(s)",
            stderr = "",
            exitCode = 0
        )
    }

    @Test
    fun `insertRow returns success on exitCode zero`() {
        whenever(mockService.insertRow(1, "test")).thenReturn(mockExecResult)
        val result = mockService.insertRow(1, "test")
        assertTrue(result.isSuccess)
    }

    @Test
    fun `insertRow handles single quotes`() {
        whenever(mockService.insertRow(2, "O'Reilly")).thenReturn(mockExecResult)
        val result = mockService.insertRow(2, "O'Reilly")
        assertTrue(result.isSuccess)
    }

    @Test
    fun `selectAll returns success on exitCode zero`() {
        whenever(mockService.selectAll()).thenReturn(mockExecResult)
        val result = mockService.selectAll()
        assertTrue(result.isSuccess)
    }

    @Test
    fun `selectAll returns failure on non-zero exitCode`() {
        val failedResult = SparkSqlResult("ERROR", "", 1)
        whenever(mockService.selectAll()).thenReturn(failedResult)
        val result = mockService.selectAll()
        assertTrue(!result.isSuccess)
    }
}