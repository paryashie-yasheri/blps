package soaringLizards.blps.integration

import org.springframework.test.context.DynamicPropertyRegistry
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.wait.strategy.Wait
import java.time.Duration
import java.net.HttpURLConnection
import java.net.URL

object IntegrationKeycloak {
    private val lock = Any()
    private var container: GenericContainer<Nothing>? = null

    private fun startContainer(): GenericContainer<Nothing> =
        GenericContainer<Nothing>("keycloak/keycloak:26.0").apply {
            withExposedPorts(8080)
            withEnv(
                mapOf(
                    "KC_DB" to "dev-file",
                    "KC_HOSTNAME_STRICT" to "false",
                    "KEYCLOAK_ADMIN" to "admin",
                    "KEYCLOAK_ADMIN_PASSWORD" to "admin",
                )
            )
            withCommand("start-dev")
            withStartupTimeout(Duration.ofMinutes(5))
            waitingFor(Wait.forListeningPort().withStartupTimeout(Duration.ofMinutes(5)))
            start()
            
            Thread.sleep(5000)
        }

    private fun ensureStarted() = synchronized(lock) {
        if (container?.isRunning != true) {
            container = startContainer()
        }
    }

    fun stop() = synchronized(lock) {
        runCatching { container?.stop() }
        container = null
    }

    fun registerProperties(registry: DynamicPropertyRegistry) {
        ensureStarted()
        val c = checkNotNull(container)
        val host = c.host
        val port = c.firstMappedPort
        
        registry.add("keycloak.server-url") { "http://$host:$port" }
        registry.add("keycloak.master.realm") { "master" }
        registry.add("keycloak.admin.username") { "admin" }
        registry.add("keycloak.admin.password") { "admin" }
        registry.add("keycloak.admin.client-id") { "admin-cli" }
        registry.add("keycloak.master.client-id") { "admin-cli" }
        registry.add("keycloak.master.client-secret") { "" }
    }

    fun getUrl(): String {
        val c = checkNotNull(container)
        return "http://${c.host}:${c.firstMappedPort}"
    }
}