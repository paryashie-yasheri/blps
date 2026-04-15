package soaringLizards.blps.integration

import org.springframework.test.context.DynamicPropertyRegistry
import org.testcontainers.containers.PostgreSQLContainer
import java.util.*

object IntegrationPostgres {
    private const val SCHEMA = "public"

    private val lock = Any()
    private var container: PostgreSQLContainer<Nothing>? = null

    private fun getOrStart(): PostgreSQLContainer<Nothing> = synchronized(lock) {
        val current = container
        if (current != null && current.isRunning) return current

        if (System.getProperty("api.version").isNullOrBlank()) {
            System.setProperty("api.version", "1.44")
        }

        val next = PostgreSQLContainer<Nothing>("postgres:15-alpine").apply {
            withDatabaseName(UUID.randomUUID().toString().replace("-", ""))
            withUsername("integration")
            withPassword("integration")
            start()
        }
        container = next
        next
    }

    fun stop() = synchronized(lock) {
        val current = container ?: return@synchronized
        runCatching { current.stop() }
        container = null
    }

    fun registerProperties(registry: DynamicPropertyRegistry) {
        val pg = getOrStart()
        val atomikosId = UUID.randomUUID().toString().replace("-", "").take(12)
        registry.add("app.datasource.xa.host", pg::getHost)
        registry.add("app.datasource.xa.port") { pg.firstMappedPort }
        registry.add("app.datasource.xa.database", pg::getDatabaseName)
        registry.add("app.datasource.xa.username", pg::getUsername)
        registry.add("app.datasource.xa.password", pg::getPassword)
        registry.add("app.datasource.xa.schema") { SCHEMA }
        registry.add("app.datasource.xa.unique-resource-name") { "test-xa-$atomikosId" }
        registry.add("app.datasource.xa.max-pool-size") { 3 }
        registry.add("app.datasource.xa.min-pool-size") { 1 }
        registry.add("app.atomikos.log-base-dir") { "${System.getProperty("java.io.tmpdir")}/blps-atomikos-tests/$atomikosId" }
        registry.add("app.atomikos.tm-unique-name") { "blps-$atomikosId" }
        registry.add("spring.liquibase.enabled") { true }
        registry.add("spring.liquibase.default-schema") { SCHEMA }
        registry.add("spring.liquibase.liquibase-schema") { SCHEMA }
        registry.add("auth.jwt.secret") { "test-integration-secret-key-0123456789" }
    }
}
