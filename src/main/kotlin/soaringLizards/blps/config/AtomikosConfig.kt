package soaringLizards.blps.config

import com.atomikos.icatch.jta.UserTransactionImp
import com.atomikos.icatch.jta.UserTransactionManager
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
import java.sql.DriverManager
import java.nio.file.Files
import java.nio.file.Path
import javax.sql.DataSource
import org.slf4j.LoggerFactory

data class XaSingleDataSourceProperties(
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

@ConfigurationProperties(prefix = "app.datasource.xa")
data class XaDataSourceProperties(
    @NestedConfigurationProperty
    var main: XaSingleDataSourceProperties = XaSingleDataSourceProperties(uniqueResourceName = "main-postgres-xa"),
    @NestedConfigurationProperty
    var journal: XaSingleDataSourceProperties = XaSingleDataSourceProperties(
        database = "journal",
        uniqueResourceName = "journal-postgres-xa",
    ),
    @NestedConfigurationProperty
    var rewards: XaSingleDataSourceProperties = XaSingleDataSourceProperties(
        database = "rewards",
        uniqueResourceName = "rewards-postgres-xa",
    ),
)

@ConfigurationProperties(prefix = "app.atomikos")
data class AtomikosRuntimeProperties(
    var logBaseDir: String = System.getProperty("java.io.tmpdir") + "/blps-atomikos",
    var tmUniqueName: String = "blps-tm",
    var transactionTimeoutSeconds: Int = 30,
    var maxTimeoutSeconds: Int = 60,
    var checkpointInterval: Long = 500,
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
    private val logger = LoggerFactory.getLogger(javaClass)

    @Bean("atomikosPropertyInitializer")
    fun atomikosPropertyInitializer(): Any {
        val logDir = Path.of(atomikosRuntimeProperties.logBaseDir)
        Files.createDirectories(logDir)
        System.setProperty("com.atomikos.icatch.output_dir", logDir.toString())
        System.setProperty("com.atomikos.icatch.log_base_dir", logDir.toString())
        System.setProperty("com.atomikos.icatch.tm_unique_name", atomikosRuntimeProperties.tmUniqueName)
        System.setProperty(
            "com.atomikos.icatch.default_jta_timeout",
            (atomikosRuntimeProperties.transactionTimeoutSeconds * 1000).toString(),
        )
        System.setProperty(
            "com.atomikos.icatch.max_timeout",
            (atomikosRuntimeProperties.maxTimeoutSeconds * 1000).toString(),
        )
        System.setProperty("com.atomikos.icatch.checkpoint_interval", atomikosRuntimeProperties.checkpointInterval.toString())
        System.setProperty("com.atomikos.icatch.threaded_2pc", "true")
        System.setProperty("com.atomikos.icatch.enable_logging", "true")
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
    fun mainDataSource(): DataSource =
        createAtomikosDataSource("main", xaDataSourceProperties.main)

    @Bean(initMethod = "init", destroyMethod = "close")
    @DependsOn("atomikosPropertyInitializer", "atomikosTransactionManager", "atomikosUserTransaction")
    fun journalDataSource(): DataSource =
        createAtomikosDataSource("journal", xaDataSourceProperties.journal)

    @Bean(initMethod = "init", destroyMethod = "close")
    @DependsOn("atomikosPropertyInitializer", "atomikosTransactionManager", "atomikosUserTransaction")
    fun rewardsDataSource(): DataSource =
        createAtomikosDataSource("rewards", xaDataSourceProperties.rewards)

    private fun createAtomikosDataSource(name: String, properties: XaSingleDataSourceProperties): DataSource {
        verifyJdbcConnectivity(name, properties)

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

    private fun verifyJdbcConnectivity(name: String, properties: XaSingleDataSourceProperties) {
        val jdbcUrl = "jdbc:postgresql://${properties.host}:${properties.port}/${properties.database}"
        val testQuery = normalizeSqlTestQuery(properties.testQuery)

        try {
            DriverManager.getConnection(jdbcUrl, properties.username, properties.password).use { connection ->
                connection.createStatement().use { statement ->
                    statement.queryTimeout = 3
                    statement.execute(testQuery)
                }
            }
            logger.info("startup_jdbc_connectivity_ok name={} url={}", name, jdbcUrl)
        } catch (ex: Exception) {
            logger.error("startup_jdbc_connectivity_failed name={} url={}", name, jdbcUrl, ex)
            throw IllegalStateException("Startup check failed: '$name' datasource is unavailable", ex)
        }
    }

    private fun normalizeSqlTestQuery(raw: String): String {
        val trimmed = raw.trim()
        if (trimmed.length >= 2) {
            val first = trimmed.first()
            val last = trimmed.last()
            if ((first == '"' && last == '"') || (first == '\'' && last == '\'')) {
                return trimmed.substring(1, trimmed.length - 1).trim()
            }
        }
        return trimmed
    }
}
