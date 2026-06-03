package soaringLizards.blps.config

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.ApplicationRunner
import org.springframework.boot.actuate.health.HealthIndicator
import org.springframework.boot.actuate.jdbc.DataSourceHealthIndicator
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.jdbc.core.JdbcTemplate
import javax.sql.DataSource

@Configuration
class DatabaseAvailabilityConfig {
    private val logger = LoggerFactory.getLogger(javaClass)

    @Bean
    fun verifyDatabasesOnStartup(
        @Qualifier("mainDataSource") mainDataSource: DataSource,
        @Qualifier("journalDataSource") journalDataSource: DataSource,
        @Qualifier("rewardsDataSource") rewardsDataSource: DataSource,
        xaDataSourceProperties: XaDataSourceProperties,
    ): ApplicationRunner {
        return ApplicationRunner {
            checkDatabase("main", mainDataSource, xaDataSourceProperties.main)
            checkDatabase("journal", journalDataSource, xaDataSourceProperties.journal)
            checkDatabase("rewards", rewardsDataSource, xaDataSourceProperties.rewards)
        }
    }

    @Bean
    fun mainDataSourceHealthIndicator(
        @Qualifier("mainDataSource") mainDataSource: DataSource,
    ): HealthIndicator =
        DataSourceHealthIndicator(mainDataSource, "SELECT 1")

    @Bean
    fun journalDataSourceHealthIndicator(
        @Qualifier("journalDataSource") journalDataSource: DataSource,
    ): HealthIndicator =
        DataSourceHealthIndicator(journalDataSource, "SELECT 1")

    @Bean
    fun rewardsDataSourceHealthIndicator(
        @Qualifier("rewardsDataSource") rewardsDataSource: DataSource,
    ): HealthIndicator =
        DataSourceHealthIndicator(rewardsDataSource, "SELECT 1")

    private fun checkDatabase(name: String, dataSource: DataSource, properties: XaSingleDataSourceProperties) {
        if (!properties.startupCheckEnabled) {
            logger.info("startup_db_check_skipped name={} required={}", name, properties.requiredOnStartup)
            return
        }

        try {
            JdbcTemplate(dataSource).apply {
                queryTimeout = properties.startupCheckTimeoutSeconds
            }.queryForObject(normalizeSqlTestQuery(properties.testQuery), Int::class.java)
            logger.info("startup_db_check_ok name={} required={}", name, properties.requiredOnStartup)
        } catch (ex: Exception) {
            if (properties.requiredOnStartup) {
                logger.error("startup_db_check_failed name={} required=true", name, ex)
                throw IllegalStateException("Startup check failed: '$name' database is unavailable", ex)
            }
            logger.warn("startup_db_check_failed name={} required=false reason={}", name, ex.message)
        }
    }

}
