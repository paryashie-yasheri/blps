package soaringLizards.blps.config

import com.atomikos.icatch.jta.UserTransactionImp
import com.atomikos.icatch.jta.UserTransactionManager
import com.atomikos.icatch.config.UserTransactionServiceImp
import com.atomikos.jdbc.AtomikosDataSourceBean
import jakarta.transaction.TransactionManager
import jakarta.transaction.UserTransaction
import org.postgresql.xa.PGXADataSource
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.context.properties.NestedConfigurationProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.DependsOn
import org.springframework.context.annotation.Primary
import org.springframework.transaction.annotation.EnableTransactionManagement
import org.springframework.transaction.jta.JtaTransactionManager
import java.nio.file.Files
import java.nio.file.Path
import java.util.Properties
import javax.sql.DataSource

data class XaSingleDataSourceProperties(
    var host: String = "",
    var port: Int = 0,
    var database: String = "",
    var username: String = "",
    var password: String = "",
    var schema: String = "",
    var uniqueResourceName: String = "",
    var minPoolSize: Int = 0,
    var maxPoolSize: Int = 0,
    var borrowConnectionTimeoutSeconds: Int = 0,
    var maintenanceIntervalSeconds: Int = 0,
    var maxIdleTimeSeconds: Int = 0,
    var testQuery: String = "",
    var startupCheckEnabled: Boolean = false,
    var requiredOnStartup: Boolean = false,
    var startupCheckTimeoutSeconds: Int = 0,
)

@ConfigurationProperties(prefix = "app.datasource.xa")
data class XaDataSourceProperties(
    @NestedConfigurationProperty
    var main: XaSingleDataSourceProperties = XaSingleDataSourceProperties(),
    @NestedConfigurationProperty
    var journal: XaSingleDataSourceProperties = XaSingleDataSourceProperties(),
    @NestedConfigurationProperty
    var rewards: XaSingleDataSourceProperties = XaSingleDataSourceProperties(),
)

@ConfigurationProperties(prefix = "app.atomikos")
data class AtomikosRuntimeProperties(
    var logBaseDir: String = System.getProperty("java.io.tmpdir") + "/blps-atomikos",
    var tmUniqueName: String = "blps-tm",
    var transactionTimeoutSeconds: Int = 30,
    var maxTimeoutSeconds: Int = 60,
    var checkpointInterval: Long = 500,
    var threaded2pc: Boolean = true,
    var enableLogging: Boolean = true,
    var forceShutdown: Boolean = false,
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
    @Bean(initMethod = "init", destroyMethod = "close")
    fun atomikosUserTransactionService(): UserTransactionServiceImp =
        UserTransactionServiceImp(atomikosProperties())

    private fun atomikosProperties(): Properties {
        val logDir = Path.of(atomikosRuntimeProperties.logBaseDir)
        Files.createDirectories(logDir)

        return mapOf(
            "com.atomikos.icatch.output_dir" to logDir,
            "com.atomikos.icatch.log_base_dir" to logDir,
            "com.atomikos.icatch.tm_unique_name" to atomikosRuntimeProperties.tmUniqueName,
            "com.atomikos.icatch.default_jta_timeout" to atomikosRuntimeProperties.transactionTimeoutSeconds * 1000,
            "com.atomikos.icatch.max_timeout" to atomikosRuntimeProperties.maxTimeoutSeconds * 1000,
            "com.atomikos.icatch.checkpoint_interval" to atomikosRuntimeProperties.checkpointInterval,
            "com.atomikos.icatch.threaded_2pc" to atomikosRuntimeProperties.threaded2pc,
            "com.atomikos.icatch.enable_logging" to atomikosRuntimeProperties.enableLogging,
        ).toProperties()
    }

    @Bean(initMethod = "init", destroyMethod = "close")
    @DependsOn("atomikosUserTransactionService")
    fun atomikosTransactionManager(): UserTransactionManager =
        UserTransactionManager().apply {
            startupTransactionService = false
            forceShutdown = atomikosRuntimeProperties.forceShutdown
        }

    @Bean
    @DependsOn("atomikosUserTransactionService")
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
    @DependsOn("atomikosUserTransactionService", "atomikosTransactionManager", "atomikosUserTransaction")
    fun mainDataSource(): DataSource =
        createAtomikosDataSource(xaDataSourceProperties.main)

    @Bean(initMethod = "init", destroyMethod = "close")
    @DependsOn("atomikosUserTransactionService", "atomikosTransactionManager", "atomikosUserTransaction")
    fun journalDataSource(): DataSource =
        createAtomikosDataSource(xaDataSourceProperties.journal)

    @Bean(initMethod = "init", destroyMethod = "close")
    @DependsOn("atomikosUserTransactionService", "atomikosTransactionManager", "atomikosUserTransaction")
    fun rewardsDataSource(): DataSource =
        createAtomikosDataSource(xaDataSourceProperties.rewards)

    private fun createAtomikosDataSource(properties: XaSingleDataSourceProperties): DataSource {
        val xaDataSource = PGXADataSource().apply {
            serverNames = arrayOf(properties.host)
            portNumbers = intArrayOf(properties.port)
            databaseName = properties.database
            user = properties.username
            password = properties.password
            currentSchema = properties.schema
        }

        return AtomikosDataSourceBean().apply {
            uniqueResourceName = properties.uniqueResourceName
            this.xaDataSource = xaDataSource
            minPoolSize = properties.minPoolSize
            maxPoolSize = properties.maxPoolSize
            borrowConnectionTimeout = properties.borrowConnectionTimeoutSeconds
            maintenanceInterval = properties.maintenanceIntervalSeconds
            maxIdleTime = properties.maxIdleTimeSeconds
            testQuery = normalizeSqlTestQuery(properties.testQuery)
        }
    }
}

private fun Map<String, Any>.toProperties(): Properties =
    Properties().also { properties ->
        forEach { (key, value) -> properties[key] = value.toString() }
    }
