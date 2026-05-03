package soaringLizards.blps.config

import org.keycloak.admin.client.Keycloak
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class KeycloakConfig(
    @Value("\${keycloak.server-url}") val serverUrl: String,
    @Value("\${keycloak.admin.username}") val adminUsername: String,
    @Value("\${keycloak.admin.password}") val adminPassword: String,
    @Value("\${keycloak.admin.client-id}") val adminClientId: String,
    @Value("\${keycloak.master.realm}") val masterRealm: String,

    ) {


    @Bean
    fun keycloakMaster(): Keycloak =
        Keycloak.getInstance(
            serverUrl,
            masterRealm,
            adminUsername,
            adminPassword,
            "admin-cli",
        )
}
