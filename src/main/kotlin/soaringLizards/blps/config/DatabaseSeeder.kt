package soaringLizards.blps.config

import org.slf4j.LoggerFactory
import org.springframework.boot.CommandLineRunner
import org.springframework.stereotype.Component
import soaringLizards.blps.dto.user.RegisterDto
import soaringLizards.blps.exception.UserAlreadyExistAuthenticationException
import soaringLizards.blps.service.UserService

@Component
class DatabaseSeeder(
    private val userService: UserService,
    private val adminCredentialsProperties: AdminCredentialsProperties,
) : CommandLineRunner {
    companion object {
        private val logger = LoggerFactory.getLogger(DatabaseSeeder::class.java)
    }

    override fun run(vararg args: String) {
        try {
            val adminRegisterDto = RegisterDto(
                adminCredentialsProperties.username,
                adminCredentialsProperties.password,
                adminCredentialsProperties.email,
            )
            userService.registerUser(adminRegisterDto, "ADMIN")
            logger.info("Default Admin User Created!")
        } catch (e: UserAlreadyExistAuthenticationException) {
            logger.info("Admin User already exists.")
        }
    }
}
