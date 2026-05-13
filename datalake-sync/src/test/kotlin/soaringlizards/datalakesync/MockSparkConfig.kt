package soaringlizards.datalakesync

import org.mockito.kotlin.mock
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.web.client.RestClient
import soaringlizards.datalakesync.spark.DevSparkSqlService
import soaringlizards.datalakesync.spark.SparkSqlExecutor
import soaringlizards.datalakesync.spark.iceberg.LedgerDataLakeService
import soaringlizards.datalakesync.spark.iceberg.LedgerTableManager

@TestConfiguration
class MockSparkConfig {

    @Bean
    @Primary
    fun sparkSqlService(): DevSparkSqlService = mock()

    @Bean
    @Primary
    fun sparkSqlExecutor(): SparkSqlExecutor = mock()

    @Bean
    @Primary
    fun restClient(): RestClient = mock()

    @Bean
    @Primary
    fun ledgerTableManager(): LedgerTableManager = mock()

    @Bean
    @Primary
    fun ledgerDataLakeService(): LedgerDataLakeService = mock()
}
