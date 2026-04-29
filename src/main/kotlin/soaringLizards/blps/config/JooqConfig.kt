package soaringLizards.blps.config

import org.jooq.DSLContext
import org.jooq.SQLDialect
import org.jooq.conf.Settings
import org.jooq.impl.DSL
import org.jooq.impl.DataSourceConnectionProvider
import org.jooq.impl.DefaultConfiguration
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.jooq.autoconfigure.SpringTransactionProvider
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.jdbc.datasource.TransactionAwareDataSourceProxy
import org.springframework.transaction.PlatformTransactionManager
import javax.sql.DataSource

@Configuration
class JooqConfig(
    @Qualifier("mainDataSource")
    private val mainDataSource: DataSource,
    @Qualifier("journalDataSource")
    private val journalDataSource: DataSource,
    @Qualifier("rewardsDataSource")
    private val rewardsDataSource: DataSource,
    private val txManager: PlatformTransactionManager,
) {

    @Bean
    @Primary
    fun dslContext(): DSLContext =
        createDslContext(mainDataSource)

    @Bean
    fun journalDslContext(): DSLContext =
        createDslContext(journalDataSource)

    @Bean
    fun rewardsDslContext(): DSLContext =
        createDslContext(rewardsDataSource)

    private fun createDslContext(dataSource: DataSource): DSLContext {
        val txAware = TransactionAwareDataSourceProxy(dataSource)
        val connectionProvider = DataSourceConnectionProvider(txAware)

        val configuration = DefaultConfiguration().apply {
            setSQLDialect(SQLDialect.POSTGRES)
            setConnectionProvider(connectionProvider)
            setTransactionProvider(SpringTransactionProvider(txManager))
            setSettings(
                Settings()
                    .withRenderSchema(false)
                    .withReturnRecordToPojo(true)
                    .withReturnIdentityOnUpdatableRecord(true)
                    .withReturnAllOnUpdatableRecord(true),
            )
        }
        return DSL.using(configuration)
    }
}
