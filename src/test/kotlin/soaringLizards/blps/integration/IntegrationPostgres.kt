package soaringLizards.blps.integration

import org.springframework.test.context.DynamicPropertyRegistry
import org.testcontainers.containers.PostgreSQLContainer
import java.util.UUID

object IntegrationPostgres {
    private const val SCHEMA = "public"

    private val lock = Any()
    private var mainContainer: PostgreSQLContainer<Nothing>? = null
    private var journalContainer: PostgreSQLContainer<Nothing>? = null

    private fun startContainer(dbPrefix: String): PostgreSQLContainer<Nothing> =
        PostgreSQLContainer<Nothing>("postgres:15-alpine").apply {
            withDatabaseName("${dbPrefix}_${UUID.randomUUID().toString().replace("-", "")}")
            withUsername("integration")
            withPassword("integration")
            withCommand("postgres", "-c", "max_prepared_transactions=100")
            start()
        }

    private fun ensureStarted() = synchronized(lock) {
        if (System.getProperty("api.version").isNullOrBlank()) {
            System.setProperty("api.version", "1.44")
        }

        if (mainContainer?.isRunning != true) {
            mainContainer = startContainer("main")
        }

        if (journalContainer?.isRunning != true) {
            journalContainer = startContainer("journal")
        }
    }

    fun stop() = synchronized(lock) {
        runCatching { mainContainer?.stop() }
        runCatching { journalContainer?.stop() }
        mainContainer = null
        journalContainer = null
    }

    fun registerProperties(registry: DynamicPropertyRegistry) {
        ensureStarted()
        val main = checkNotNull(mainContainer)
        val journal = checkNotNull(journalContainer)
        val atomikosId = UUID.randomUUID().toString().replace("-", "").take(12)

        registry.add("app.datasource.xa.main.host", main::getHost)
        registry.add("app.datasource.xa.main.port") { main.firstMappedPort }
        registry.add("app.datasource.xa.main.database", main::getDatabaseName)
        registry.add("app.datasource.xa.main.username", main::getUsername)
        registry.add("app.datasource.xa.main.password", main::getPassword)
        registry.add("app.datasource.xa.main.schema") { SCHEMA }
        registry.add("app.datasource.xa.main.unique-resource-name") { "test-main-xa-$atomikosId" }
        registry.add("app.datasource.xa.main.max-pool-size") { 3 }
        registry.add("app.datasource.xa.main.min-pool-size") { 1 }

        registry.add("app.datasource.xa.journal.host", journal::getHost)
        registry.add("app.datasource.xa.journal.port") { journal.firstMappedPort }
        registry.add("app.datasource.xa.journal.database", journal::getDatabaseName)
        registry.add("app.datasource.xa.journal.username", journal::getUsername)
        registry.add("app.datasource.xa.journal.password", journal::getPassword)
        registry.add("app.datasource.xa.journal.schema") { SCHEMA }
        registry.add("app.datasource.xa.journal.unique-resource-name") { "test-journal-xa-$atomikosId" }
        registry.add("app.datasource.xa.journal.max-pool-size") { 3 }
        registry.add("app.datasource.xa.journal.min-pool-size") { 1 }

        registry.add("app.atomikos.log-base-dir") {
            "${System.getProperty("java.io.tmpdir")}/blps-atomikos-tests/$atomikosId"
        }
        registry.add("app.atomikos.tm-unique-name") { "blps-$atomikosId" }
        registry.add("spring.liquibase.enabled") { true }
        registry.add("spring.liquibase.default-schema") { SCHEMA }
        registry.add("spring.liquibase.liquibase-schema") { SCHEMA }
        registry.add("app.journal.liquibase.enabled") { true }
        registry.add("app.journal.liquibase.default-schema") { SCHEMA }
        registry.add("app.journal.liquibase.liquibase-schema") { SCHEMA }
        registry.add("auth.jwt.secret") { "test-integration-secret-key-0123456789" }
    }
}
