package soaringLizards.blps.service

import org.slf4j.LoggerFactory
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.RequestBody
import soaringLizards.blps.config.EdxSessionAuthManager
import soaringLizards.blps.controller.AuthController
import soaringLizards.blps.dto.user.*
import soaringLizards.blps.dto.userBalances.UserBalanceDto
import soaringLizards.blps.exception.UserAlreadyExistAuthenticationException
import soaringLizards.blps.exception.UserNotFoundException
import soaringLizards.blps.jooq.generated.tables.records.UsersRecord
import soaringLizards.blps.repository.UserBalanceRepository
import soaringLizards.blps.repository.UserRepository
import java.time.Instant

private val logger = LoggerFactory.getLogger(AuthController::class.java)

@Service
class UserService(
    private val passwordEncoder: PasswordEncoder,
    private val userRepository: UserRepository,
    private val userBalanceRepository: UserBalanceRepository,
    private val jwtTokenService: JwtTokenService,
    private val edxSessionAuthManager: EdxSessionAuthManager,
    private val keycloakService: KeycloakService,
    private val transactionTemplate: org.springframework.transaction.support.TransactionTemplate,
) {
    fun getUserByUsername(username: String): UserResponse? {
        val user = userRepository.findByUsername(username)
        return user?.toUserResponse()
    }

    fun authenticateStaff(username: String, password: String): UsersRecord {
        val user = userRepository.findByUsername(username)
            ?: throw UserNotFoundException("User not found in database")

        if (user.role != "STAFF" && user.role != "ADMIN")
            throw AccessDeniedException("User not authorized to access")

        if (!passwordEncoder.matches(password, user.passwordHash))
            throw AccessDeniedException("Wrong password")

        return user
    }

    fun loginStaff(request: LoginRequestDto): LoginResponseDto {
        val user = authenticateStaff(request.username, request.password)

        val token = jwtTokenService.generateToken(
            user.id!!,
            user.username!!,
            Email(user.email!!),
            setOf(user.role!!),
        )

        return LoginResponseDto(
            accessToken = token,
            tokenType = "Bearer",
            expiresIn = 3600,
            username = user.username!!,
            role = user.role!!,
        )
    }

    fun authenticateOrCreateOpenedxUser(username: String, email: Email): UsersRecord {
        val existingUser = userRepository.findByUsername(username)

        return existingUser ?: let {
            // Delegate deterministic password generation to KeycloakService
            // to ensure a single, production-ready double-hashing implementation
            val deterministicPassword = keycloakService.generateDeterministicPassword(username)

            val newUser = RegisterDto(
                username = username,
                password = deterministicPassword,
                email = email,
            )
            registerUser(newUser, "OPENEDX_USER")
        }
    }

    @Transactional
    fun registerUser(request: RegisterDto, role: String): UsersRecord {
        if (userRepository.existWithUsername(request.username))
            throw UserAlreadyExistAuthenticationException("User already exists")

        val newUserDto = UserDto(
            request.username,
            passwordEncoder.encode(request.password)!!,
            request.email,
            role,
        )

        val newUser = userRepository.insert(newUserDto)
            ?: throw UserAlreadyExistAuthenticationException("User already exists")

        userBalanceRepository.insert(UserBalanceDto.createDefault(newUser.id!!))

        // Sync to Keycloak based on role
        if (role == "OPENEDX_USER") {
            keycloakService.createEdxUser(
                username = newUser.username!!,
                email = newUser.email!!,
                edxSession = null,
            )
        } else {
            keycloakService.createStaffUser(
                username = newUser.username!!,
                email = newUser.email!!,
                userId = newUser.id!!.toString(),
                role = role,
                password = request.password,
            )
        }

        return newUser
    }

    fun loginOpenedxUser(@RequestBody request: LoginRequestDto): LoginResponseDto {
        logger.info("Login attempt for user: ${request.username}")

        val session = edxSessionAuthManager.authenticateUser(request.username, request.password)

        val email = edxSessionAuthManager.getUserEmail(session, request.username)
            ?: "placeholder_${request.username}@mail.com"

        return transactionTemplate.execute {
            val user = authenticateOrCreateOpenedxUser(request.username, Email(email))
            keycloakService.createEdxUser(
                username = request.username,
                email = email,
                edxSession = session,
            )
            val token = jwtTokenService.generateToken(
                user.id!!,
                user.username!!,
                Email(user.email!!),
                setOf("OPENEDX_USER"),
                session,
            )
            LoginResponseDto(
                accessToken = token,
                tokenType = "Bearer",
                expiresIn = session.expiresAt.epochSecond - Instant.now().epochSecond,
                username = user.username!!,
                role = "OPENEDX_USER",
            )
        }!!
    }
}
