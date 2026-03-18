package soaringLizards.blps.config

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.CommandLineRunner
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component
import soaringLizards.blps.dto.user.Email
import soaringLizards.blps.dto.user.UserDto
import soaringLizards.blps.exception.UserAlreadyExistAuthenticationException
import soaringLizards.blps.repository.UserRepository
import soaringLizards.blps.service.UserService
import java.util.Objects.isNull

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
            val admin = UserDto(
                adminCredentialsProperties.username,
                adminCredentialsProperties.password,
                Email(adminCredentialsProperties.email),
                "ADMIN",
            )
            userService
            logger.info("Default Admin User Created!")
        } catch (e: UserAlreadyExistAuthenticationException) {
            logger.info("Admin User already exists.")
        }
    }
}