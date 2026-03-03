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
        registry.add("spring.datasource.url") { "${pg.jdbcUrl}?currentSchema=$SCHEMA" }
        registry.add("spring.datasource.username", pg::getUsername)
        registry.add("spring.datasource.password", pg::getPassword)
        registry.add("spring.datasource.driver-class-name", pg::getDriverClassName)
        registry.add("spring.liquibase.enabled") { true }
        registry.add("spring.liquibase.default-schema") { SCHEMA }
        registry.add("spring.liquibase.liquibase-schema") { SCHEMA }
        registry.add("spring.datasource.hikari.maximum-pool-size") { 3 }
        registry.add("spring.datasource.hikari.minimum-idle") { 1 }
        registry.add("auth.jwt.secret") { "test-integration-secret-key-0123456789" }
    }
}
