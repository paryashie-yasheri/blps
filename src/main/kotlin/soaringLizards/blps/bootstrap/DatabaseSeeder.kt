package soaringLizards.blps.bootstrap

import org.keycloak.admin.client.Keycloak
import org.slf4j.LoggerFactory
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.ApplicationListener
import org.springframework.stereotype.Component
import soaringLizards.blps.config.KeycloakConfig
import soaringLizards.blps.config.properties.AdminCredentialsProperties
import soaringLizards.blps.dto.user.RegisterDto
import soaringLizards.blps.exception.UserAlreadyExistAuthenticationException
import soaringLizards.blps.service.KeycloakService

@Component
class DatabaseSeeder(
    private val keycloakService: KeycloakService,
    private val keycloakConfig: KeycloakConfig,
    private val adminCredentialsProperties: AdminCredentialsProperties,
) : ApplicationListener<ApplicationReadyEvent> {

    companion object {
        private val logger = LoggerFactory.getLogger(DatabaseSeeder::class.java)
    }

    fun keycloakMaster(): Keycloak =
        Keycloak.getInstance(
            keycloakConfig.serverUrl,
            "master",
            keycloakConfig.adminUsername,
            keycloakConfig.adminPassword,
            "admin-cli",
        )


    override fun onApplicationEvent(event: ApplicationReadyEvent) {
        try {
            val adminRegisterDto = RegisterDto(
                adminCredentialsProperties.username,
                adminCredentialsProperties.password,
                adminCredentialsProperties.email,
            )
            keycloakService.registerUser(adminRegisterDto, "ADMIN", keycloakMaster())

            logger.info("Default Admin User Created!")
        } catch (_: UserAlreadyExistAuthenticationException) {
            logger.info("Admin User already exists – skipping creation")
        } catch (ex: Exception) {
            logger.error("Failed to seed admin user", ex)
        }
    }
}
