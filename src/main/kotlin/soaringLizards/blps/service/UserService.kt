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
import soaringLizards.blps.exception.BusinessLogicException
import soaringLizards.blps.exception.LmsException
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

        if (passwordEncoder.matches(password, user.passwordHash))
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
            expiresIn = jwtTokenService.staffJwtExpirationMinutes * 60,
            username = user.username!!,
            role = user.role!!,
        )
    }

    fun authenticateOrCreateOpenedxUser(username: String, email: Email): UsersRecord {
        val existingUser = userRepository.findByUsername(username)

        return if (existingUser != null) {
            existingUser
        } else {
            val newUser = RegisterDto(
                username = username,
                password = "i will be changed", //TODO
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

        return newUser
    }


    @Transactional
    fun loginOpenedxUser(@RequestBody request: LoginRequestDto): LoginResponseDto {
        logger.info("Login attempt for user: ${request.username}")

        val lmsResult = try {
            edxSessionAuthManager.authenticateUser(request.username, request.password)
        } catch (e: LmsException) {
            throw e
        } catch (e: Exception) {
            logger.error("LMS authentication error: ${e.message}")
            throw BusinessLogicException("LMS_UNAVAILABLE, message: ${e.message}")
        }

        val email = edxSessionAuthManager.getUserEmail(lmsResult, request.username)
            ?: "placeholder_${request.username}@mail.com"

        val user = authenticateOrCreateOpenedxUser(
            request.username,
            Email(email),
        )

        val token = jwtTokenService.generateToken(
            user.id!!,
            user.username!!,
            Email(user.email!!),
            setOf("OPENEDX_USER"),
            lmsResult,
        )

        return LoginResponseDto(
            accessToken = token,
            tokenType = "Bearer",
            expiresIn = lmsResult.expiresAt.epochSecond - Instant.now().epochSecond,
            username = user.username!!,
            role = "OPENEDX_USER",
        )
    }
}
