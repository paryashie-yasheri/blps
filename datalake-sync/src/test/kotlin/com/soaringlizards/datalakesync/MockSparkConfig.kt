package com.soaringlizards.datalakesync

import com.soaringlizards.datalakesync.spark.SparkSqlExecutor
import com.soaringlizards.datalakesync.spark.SparkSqlResult
import com.soaringlizards.datalakesync.spark.SparkSqlService
import org.mockito.kotlin.mock
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary

@TestConfiguration
class MockSparkConfig {

    @Bean
    @Primary
    fun sparkSqlService(): SparkSqlService = mock {}

    @Bean
    @Primary
    fun sparkSqlExecutor(): SparkSqlExecutor = mock {}
}