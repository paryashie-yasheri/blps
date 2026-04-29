package soaringLizards.blps.config

import liquibase.integration.spring.SpringLiquibase
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.liquibase.autoconfigure.LiquibaseProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import javax.sql.DataSource

@Configuration
class LiquibaseConfig {

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
    ): SpringLiquibase =
        createLiquibase(mainDataSource, properties)

    @Bean
    fun journalLiquibase(
        @Qualifier("journalDataSource") journalDataSource: DataSource,
        @Qualifier("journalLiquibaseProperties") properties: LiquibaseProperties,
    ): SpringLiquibase =
        createLiquibase(journalDataSource, properties)

    @Bean
    fun rewardsLiquibase(
        @Qualifier("rewardsDataSource") rewardsDataSource: DataSource,
        @Qualifier("rewardsLiquibaseProperties") properties: LiquibaseProperties,
    ): SpringLiquibase =
        createLiquibase(rewardsDataSource, properties)

    private fun createLiquibase(
        dataSource: DataSource,
        properties: LiquibaseProperties,
    ): SpringLiquibase {
        return SpringLiquibase().apply {
            setDataSource(dataSource)
            setChangeLog(properties.changeLog)
            setContexts(properties.contexts?.joinToString(","))
            setDefaultSchema(properties.defaultSchema)
            setLiquibaseSchema(properties.liquibaseSchema)
            setLabelFilter(properties.labelFilter?.joinToString(","))
            setShouldRun(properties.isEnabled)
        }
    }
}
