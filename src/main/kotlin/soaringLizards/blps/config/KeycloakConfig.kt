package soaringLizards.blps.config

import org.keycloak.admin.client.Keycloak
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class KeycloakConfig(
    @Value("\${keycloak.server-url}") private val serverUrl: String,
    @Value("\${keycloak.admin.username}") private val adminUsername: String,
    @Value("\${keycloak.admin.password}") private val adminPassword: String,
    @Value("\${keycloak.admin.client-id}") private val adminClientId: String,
) {

    @Bean
    fun keycloakMaster(): Keycloak {
        return Keycloak.getInstance(
            serverUrl,
            "master",
            adminUsername,
            adminPassword,
            adminClientId,
        )
    }
}
