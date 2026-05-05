package soaringLizards.blps.service

import jakarta.transaction.Transactional
import org.keycloak.admin.client.Keycloak
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.support.TransactionTemplate
import soaringLizards.blps.config.EdxSessionAuthManager
import soaringLizards.blps.config.KeycloakConfig
import soaringLizards.blps.dto.edx.EdxAuthenticatedSession
import soaringLizards.blps.dto.user.*
import soaringLizards.blps.exception.BusinessLogicException
import soaringLizards.blps.exception.UserAlreadyExistAuthenticationException
import soaringLizards.blps.repository.UserRepository
import java.security.MessageDigest
import java.util.*

private val logger = LoggerFactory.getLogger(KeycloakService::class.java)


@Service
class KeycloakService(
    private val keycloakMaster: Keycloak,
    private val keycloakConfig: KeycloakConfig,

    private val edxSessionAuthManager: EdxSessionAuthManager,
    private val transactionTemplate: TransactionTemplate,

    val passwordEncoder: PasswordEncoder,
    private val userRepository: UserRepository,

    @Value("\${keycloak.server-url}") private val serverUrl: String,
    @Value("\${keycloak.master.realm}") private val masterRealm: String,
    @Value("\${keycloak.master.client-id}") private val clientId: String,
    @Value("\${keycloak.master.client-secret}") private val clientSecret: String,
) {

    @Transactional
    fun registerUser(request: RegisterDto, role: String): UserResponse {
        createStaffUserAndGetId(request.username, request.email, role, request.password)
        return UserResponse(request.username, request.email, role)
    }

    fun generateDeterministicPassword(username: String): String {
        val raw = "${username}:${clientSecret}"

        val sha256Digest = MessageDigest.getInstance("SHA-256")
            .digest(raw.toByteArray(Charsets.UTF_8))
        val sha256Hex = sha256Digest.joinToString("") { "%02x".format(it) }

        return sha256Hex
    }

    fun loginStaff(request: LoginRequestDto): LoginResponseDto {
        val response = getStaffToken(request.username, request.password)

        val role = keycloakMaster.realm(masterRealm)
            .users()
            .search(request.username)
            .first()
            .attributes["role"]
            ?.first()
            ?: throw BusinessLogicException("no attribute role found")

        return response.toLoginResponse(request.username, role)
    }

    fun getStaffToken(username: String, password: String): KeycloakTokenResponse =
        Keycloak.getInstance(serverUrl, masterRealm, username, password, clientId, clientSecret)
            .tokenManager().accessToken.toKeycloakTokenResponse()

    fun createStaffUserAndGetId(
        username: String,
        email: Email,
        role: String,
        rawPassword: String,
    ): UUID {

        val id = userRepository.userExists(username)

        if (id != null)
            throw UserAlreadyExistAuthenticationException("User $username already exists")

        return createStaffUser(
            username,
            email,
            role,
            rawPassword,
        )
    }

    fun createEdxUserOrGetId(
        username: String,
        email: Email,
        session: EdxAuthenticatedSession,
    ) =
        userRepository.userExists(username)
            ?: createEdxUser(username, email, session)

    private fun createStaffUser(
        username: String,
        email: Email,
        role: String,
        rawPassword: String,
    ) =
        userRepository.createUser(
            username,
            email,
            role,
            rawPassword,
            null,
        )

    private fun createEdxUser(
        username: String,
        email: Email,
        session: EdxAuthenticatedSession,
    ) =
        userRepository.createUser(
            username,
            email,
            "OPENEDX_USER",
            generateDeterministicPassword(username),
            session,
        )

    @Transactional
    fun loginOpenedxUser(request: LoginRequestDto): LoginResponseDto {
        logger.info("Login attempt for user: ${request.username}")

        return transactionTemplate.execute {
            val session = edxSessionAuthManager.authenticateUser(request.username, request.password)

            val email = edxSessionAuthManager.getUserEmail(session, request.username)
                ?: "placeholder_${request.username}@mail.com"

            val user = createEdxUserOrGetId(request.username, Email(email), session)

            Keycloak.getInstance(
                serverUrl,
                masterRealm,
                request.username,
                generateDeterministicPassword(request.username),
                clientId,
                clientSecret,
            ).tokenManager()
                .accessToken
                .toKeycloakTokenResponse()
                .toLoginResponse(request.username, "OPENEDX_USER")

        }
    }

    fun getUserRoles(username: String): List<String> {
        val realm = keycloakMaster.realm(masterRealm)
        val users = realm.users()
        val existing = users.search(username)
        return existing.firstOrNull()?.realmRoles ?: emptyList()
    }

    fun getAdminAccessToken(): String {
        return keycloakMaster.tokenManager().getAccessToken().token
    }

    fun getRealm(): String = masterRealm

    fun getKeycloakMaster(): Keycloak = keycloakMaster
}


