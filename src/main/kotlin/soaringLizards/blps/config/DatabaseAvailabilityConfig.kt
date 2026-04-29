package soaringLizards.blps.config

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.ApplicationRunner
import org.springframework.boot.health.contributor.HealthIndicator
import org.springframework.boot.jdbc.health.DataSourceHealthIndicator
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
    ): ApplicationRunner {
        return ApplicationRunner {
            checkDatabase("main", mainDataSource)
            checkDatabase("journal", journalDataSource)
            logger.info("startup_db_check_ok main=true journal=true")
        }
    }

    @Bean
    fun journalDataSourceHealthIndicator(
        @Qualifier("journalDataSource") journalDataSource: DataSource,
    ): HealthIndicator =
        DataSourceHealthIndicator(journalDataSource, "SELECT 1")

    private fun checkDatabase(name: String, dataSource: DataSource) {
        try {
            JdbcTemplate(dataSource).queryForObject("SELECT 1", Int::class.java)
            logger.info("startup_db_check_ok name={}", name)
        } catch (ex: Exception) {
            logger.error("startup_db_check_failed name={} reason={}", name, ex.message, ex)
            throw IllegalStateException("Startup check failed: '$name' database is unavailable", ex)
        }
    }
}
