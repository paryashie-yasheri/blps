package soaringLizards.blps.config

import org.jooq.DSLContext
import org.jooq.SQLDialect
import org.jooq.conf.Settings
import org.jooq.impl.DSL
import org.jooq.impl.DefaultConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.jdbc.datasource.TransactionAwareDataSourceProxy
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.boot.jooq.autoconfigure.SpringTransactionProvider
import javax.sql.DataSource

@Configuration
class JooqConfig(
    private val dataSource: DataSource,
    private val txManager: PlatformTransactionManager,
) {

    @Bean
    fun dslContext(): DSLContext {
        val txAware = TransactionAwareDataSourceProxy(dataSource)
        val connectionProvider = org.jooq.impl.DataSourceConnectionProvider(txAware)

        val configuration = DefaultConfiguration().apply {
            setSQLDialect(SQLDialect.POSTGRES)
            setConnectionProvider(connectionProvider)
            setTransactionProvider(
                SpringTransactionProvider(txManager),
            )
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
