package soaringLizards.blps.config

import liquibase.integration.spring.SpringLiquibase
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.liquibase.autoconfigure.LiquibaseProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import javax.sql.DataSource

@Configuration
class LiquibaseConfig {
    private val logger = LoggerFactory.getLogger(javaClass)

    @Bean
    @ConfigurationProperties("spring.liquibase")
    fun mainLiquibaseProperties(): LiquibaseProperties =
        LiquibaseProperties()

    @Bean
    @ConfigurationProperties("app.journal.liquibase")
    fun journalLiquibaseProperties(): LiquibaseProperties =
        LiquibaseProperties().apply {
            changeLog = "classpath:db/journal/db.journal.changelog-master.yaml"
        }

    @Bean
    @ConfigurationProperties("app.rewards.liquibase")
    fun rewardsLiquibaseProperties(): LiquibaseProperties =
        LiquibaseProperties().apply {
            changeLog = "classpath:db/rewards/db.rewards.changelog-master.yaml"
        }

    @Bean
    fun mainLiquibase(
        @Qualifier("mainDataSource") mainDataSource: DataSource,
        @Qualifier("mainLiquibaseProperties") properties: LiquibaseProperties,
        xaDataSourceProperties: XaDataSourceProperties,
    ): SpringLiquibase =
        createLiquibase("main", mainDataSource, properties, xaDataSourceProperties.main.requiredOnStartup)

    @Bean
    fun journalLiquibase(
        @Qualifier("journalDataSource") journalDataSource: DataSource,
        @Qualifier("journalLiquibaseProperties") properties: LiquibaseProperties,
        xaDataSourceProperties: XaDataSourceProperties,
    ): SpringLiquibase =
        createLiquibase("journal", journalDataSource, properties, xaDataSourceProperties.journal.requiredOnStartup)

    @Bean
    fun rewardsLiquibase(
        @Qualifier("rewardsDataSource") rewardsDataSource: DataSource,
        @Qualifier("rewardsLiquibaseProperties") properties: LiquibaseProperties,
        xaDataSourceProperties: XaDataSourceProperties,
    ): SpringLiquibase =
        createLiquibase("rewards", rewardsDataSource, properties, xaDataSourceProperties.rewards.requiredOnStartup)

    private fun createLiquibase(
        name: String,
        dataSource: DataSource,
        properties: LiquibaseProperties,
        requiredOnStartup: Boolean,
    ): SpringLiquibase {
        return DegradableSpringLiquibase(name, requiredOnStartup).apply {
            setDataSource(dataSource)
            setChangeLog(properties.changeLog)
            setContexts(properties.contexts?.joinToString(","))
            setDefaultSchema(properties.defaultSchema)
            setLiquibaseSchema(properties.liquibaseSchema)
            setLabelFilter(properties.labelFilter?.joinToString(","))
            setShouldRun(properties.isEnabled)
        }
    }

    private inner class DegradableSpringLiquibase(
        private val name: String,
        private val requiredOnStartup: Boolean,
    ) : SpringLiquibase() {
        override fun afterPropertiesSet() {
            try {
                super.afterPropertiesSet()
            } catch (ex: Exception) {
                if (requiredOnStartup) {
                    throw ex
                }
                logger.warn("liquibase_skipped_unavailable_database name={} reason={}", name, ex.message)
            }
        }
    }
}
