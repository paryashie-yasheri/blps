package soaringLizards.blps.service

import net.logstash.logback.argument.StructuredArguments.kv
import org.slf4j.LoggerFactory
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import soaringLizards.blps.auth.JwtService
import soaringLizards.blps.error.BadRequestException
import soaringLizards.blps.error.ConflictException
import soaringLizards.blps.error.ForbiddenException
import soaringLizards.blps.error.UnauthorizedException
import soaringLizards.blps.model.dto.AuthResponse
import soaringLizards.blps.model.dto.RegisterRequest
import soaringLizards.blps.model.user.User
//import soaringLizards.blps.repository.ProfileRepository
import soaringLizards.blps.repository.UserRepository
import java.util.*

@Service
class AuthService(
    private val passwordEncoder: PasswordEncoder,
    private val jwtService: JwtService,
    private val userRepository: UserRepository,
//    private val profileRepository: ProfileRepository,
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    fun authenticate(login: String, password: String): AuthResponse {
        val user = resolveUser(login) ?: throw UnauthorizedException("Invalid credentials")
        if (user.role.trim().lowercase() == "blocked") throw ForbiddenException("User is blocked")
        if (!passwordEncoder.matches(password, user.passwordHash)) throw UnauthorizedException("Invalid credentials")
        val token = jwtService.generateToken(user.id, listOf(user.role))
        logger.info("auth_login_success", kv("userId", user.id), kv("role", user.role))
        return AuthResponse(token.token, token.expiresAt)
    }

    private fun resolveUser(login: String): User? {
        val normalized = login.trim()
        if (normalized.isEmpty()) return null

        val userByEmail = if (normalized.contains("@")) userRepository.findByEmail(normalized) else null
        if (userByEmail != null) return userByEmail

        val profile = profileRepository.findByUsername(normalized.lowercase())
        if (profile != null) return userRepository.findById(profile.userId)

        return userRepository.findById(normalized)
    }

    @Transactional
    fun register(request: RegisterRequest): AuthResponse {
        val email = request.email.trim()
        if (email.isBlank()) throw BadRequestException("Email must not be blank")
        val normalizedEmail = email.lowercase()
        if (request.password.isBlank()) throw BadRequestException("Password must not be blank")

        val userId = request.userId?.takeIf { it.isNotBlank() } ?: UUID.randomUUID().toString()
        val username = request.username.trim().lowercase()
            .ifBlank { throw BadRequestException("Username must not be blank") }

        val exists = userRepository.existsByIdOrEmail(userId, normalizedEmail)
//                || profileRepository.existsByUsername(username)
        if (exists) throw ConflictException("User already exists")

        val passwordHash = passwordEncoder.encode(request.password)
            ?: throw IllegalStateException("PasswordEncoder.encode returned null")

        userRepository.insert(userId, normalizedEmail, "user", request.phone, passwordHash)
//        profileRepository.insert(UUID.randomUUID().toString(), userId, username, request.displayName)

        val token = jwtService.generateToken(userId, listOf("user"))
        logger.info("auth_register_success", kv("userId", userId))
        return AuthResponse(token.token, token.expiresAt)
    }
}