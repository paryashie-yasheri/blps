package soaringLizards.blps.bootstrap

import org.slf4j.LoggerFactory
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.ApplicationListener
import org.springframework.stereotype.Component
import soaringLizards.blps.config.AdminCredentialsProperties
import soaringLizards.blps.dto.user.RegisterDto
import soaringLizards.blps.exception.UserAlreadyExistAuthenticationException
import soaringLizards.blps.service.UserService

@Component
class DatabaseSeeder(
    private val userService: UserService,
    private val adminCredentialsProperties: AdminCredentialsProperties,
) : ApplicationListener<ApplicationReadyEvent> {

    companion object {
        private val logger = LoggerFactory.getLogger(DatabaseSeeder::class.java)
    }

    override fun onApplicationEvent(event: ApplicationReadyEvent) {
        try {
            val adminRegisterDto = RegisterDto(
                adminCredentialsProperties.username,
                adminCredentialsProperties.password,
                adminCredentialsProperties.email,
            )
            userService.registerUser(adminRegisterDto, "ADMIN")

            logger.info("Default Admin User Created!")
        } catch (_: UserAlreadyExistAuthenticationException) {
            logger.info("Admin User already exists – skipping creation")
        } catch (ex: Exception) {
            logger.error("Failed to seed admin user", ex)
        }
    }
}
