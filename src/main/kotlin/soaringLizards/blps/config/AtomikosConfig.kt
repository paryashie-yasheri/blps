package soaringLizards.blps.config

import com.atomikos.icatch.jta.UserTransactionImp
import com.atomikos.icatch.jta.UserTransactionManager
import com.atomikos.jdbc.AtomikosDataSourceBean
import jakarta.transaction.TransactionManager
import jakarta.transaction.UserTransaction
import org.postgresql.xa.PGXADataSource
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.DependsOn
import org.springframework.context.annotation.Primary
import org.springframework.transaction.annotation.EnableTransactionManagement
import org.springframework.transaction.jta.JtaTransactionManager
import java.nio.file.Files
import java.nio.file.Path
import javax.sql.DataSource

@ConfigurationProperties(prefix = "app.datasource.xa")
data class XaDataSourceProperties(
    var host: String = "localhost",
    var port: Int = 5432,
    var database: String = "studs",
    var username: String = "",
    var password: String = "",
    var schema: String = "public",
    var uniqueResourceName: String = "main-postgres-xa",
    var minPoolSize: Int = 1,
    var maxPoolSize: Int = 8,
    var borrowConnectionTimeoutSeconds: Int = 30,
    var maintenanceIntervalSeconds: Int = 60,
    var maxIdleTimeSeconds: Int = 60,
    var testQuery: String = "SELECT 1",
)

@ConfigurationProperties(prefix = "app.atomikos")
data class AtomikosRuntimeProperties(
    var logBaseDir: String = System.getProperty("java.io.tmpdir") + "/blps-atomikos",
    var tmUniqueName: String = "blps-tm",
    var transactionTimeoutSeconds: Int = 30,
)

@Configuration
@EnableTransactionManagement
@EnableConfigurationProperties(
    XaDataSourceProperties::class,
    AtomikosRuntimeProperties::class,
)
class AtomikosConfig(
    private val xaDataSourceProperties: XaDataSourceProperties,
    private val atomikosRuntimeProperties: AtomikosRuntimeProperties,
) {

    @Bean("atomikosPropertyInitializer")
    fun atomikosPropertyInitializer(): Any {
        val logDir = Path.of(atomikosRuntimeProperties.logBaseDir)
        Files.createDirectories(logDir)
        System.setProperty("com.atomikos.icatch.output_dir", logDir.toString())
        System.setProperty("com.atomikos.icatch.log_base_dir", logDir.toString())
        System.setProperty("com.atomikos.icatch.tm_unique_name", atomikosRuntimeProperties.tmUniqueName)
        return Any()
    }

    @Bean(initMethod = "init", destroyMethod = "close")
    @DependsOn("atomikosPropertyInitializer")
    fun atomikosTransactionManager(): UserTransactionManager =
        UserTransactionManager().apply {
            forceShutdown = false
        }

    @Bean
    @DependsOn("atomikosPropertyInitializer")
    fun atomikosUserTransaction(): UserTransaction =
        UserTransactionImp().apply {
            setTransactionTimeout(atomikosRuntimeProperties.transactionTimeoutSeconds)
        }

    @Bean(name = ["transactionManager"])
    @Primary
    fun transactionManager(
        atomikosUserTransaction: UserTransaction,
        atomikosTransactionManager: TransactionManager,
    ): JtaTransactionManager =
        JtaTransactionManager(atomikosUserTransaction, atomikosTransactionManager)

    @Bean(initMethod = "init", destroyMethod = "close")
    @Primary
    @DependsOn("atomikosPropertyInitializer", "atomikosTransactionManager", "atomikosUserTransaction")
    fun dataSource(): DataSource {
        val xaDataSource = PGXADataSource().apply {
            serverNames = arrayOf(xaDataSourceProperties.host)
            portNumbers = intArrayOf(xaDataSourceProperties.port)
            databaseName = xaDataSourceProperties.database
            user = xaDataSourceProperties.username
            password = xaDataSourceProperties.password
            currentSchema = xaDataSourceProperties.schema
        }

        return AtomikosDataSourceBean().apply {
            uniqueResourceName = xaDataSourceProperties.uniqueResourceName
            this.xaDataSource = xaDataSource
            minPoolSize = xaDataSourceProperties.minPoolSize
            maxPoolSize = xaDataSourceProperties.maxPoolSize
            borrowConnectionTimeout = xaDataSourceProperties.borrowConnectionTimeoutSeconds
            maintenanceInterval = xaDataSourceProperties.maintenanceIntervalSeconds
            maxIdleTime = xaDataSourceProperties.maxIdleTimeSeconds
            testQuery = xaDataSourceProperties.testQuery
        }
    }
}
