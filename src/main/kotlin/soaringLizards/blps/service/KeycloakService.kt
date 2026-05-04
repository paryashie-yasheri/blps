package soaringLizards.blps.service

import jakarta.transaction.Transactional
import org.keycloak.admin.client.Keycloak
import org.keycloak.representations.idm.CredentialRepresentation
import org.keycloak.representations.idm.UserRepresentation
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.util.LinkedMultiValueMap
import org.springframework.web.client.RestClient
import soaringLizards.blps.config.EdxSessionAuthManager
import soaringLizards.blps.config.KeycloakJwtDecoder
import soaringLizards.blps.dto.edx.EdxAuthenticatedSession
import soaringLizards.blps.dto.user.*
import soaringLizards.blps.exception.BusinessLogicException
import soaringLizards.blps.exception.UserAlreadyExistAuthenticationException
import java.security.MessageDigest
import java.util.*

@Service
class KeycloakService(
    private val keycloakMaster: Keycloak,
    private val passwordEncoder: PasswordEncoder,

    private val jwtDecoder: KeycloakJwtDecoder,

    private val edxSessionAuthManager: EdxSessionAuthManager,

    @Value("\${keycloak.server-url}") private val serverUrl: String,
    @Value("\${keycloak.master.realm}") private val masterRealm: String,
    @Value("\${keycloak.master.client-id}") private val clientId: String,
    @Value("\${keycloak.master.client-secret}") private val clientSecret: String,
) {

    @Transactional
    fun registerUser(request: RegisterDto, role: String, keycloak: Keycloak? = null): UserResponse {
        createUserAndGetId(request.username, request.email, role, request.password, keycloak)
        return UserResponse(request.username, request.email, role)
    }

    private val restClient = RestClient.builder().baseUrl(serverUrl).build()

    fun generateDeterministicPassword(username: String): String {
        val raw = "${username}:${clientSecret}"

        val sha256Digest = MessageDigest.getInstance("SHA-256")
            .digest(raw.toByteArray(Charsets.UTF_8))
        val sha256Hex = sha256Digest.joinToString("") { "%02x".format(it) }

        return passwordEncoder.encode(sha256Hex)
            ?: throw IllegalStateException("Password encoding failed for user: $username")
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

    fun getEdxToken(username: String): KeycloakTokenResponse {
        val formData = LinkedMultiValueMap<String, String>().apply {
            add("grant_type", "client_credentials")
            add("client_id", clientId)
            add("client_secret", clientSecret)
            add("scope", "openid profile email")
        }

        return restClient.post()
            .uri("/realms/${masterRealm}/protocol/openid-connect/token")
            .header("Content-Type", "application/x-www-form-urlencoded")
            .body(formData)
            .retrieve()
            .body(KeycloakTokenResponse::class.java)
            ?: throw IllegalStateException("Failed to get edx token from Keycloak")
    }

    fun createEdxUser(username: String, email: String, edxSession: EdxAuthenticatedSession?) {
        val realm = keycloakMaster.realm(masterRealm)
        val users = realm.users()

        val existing = users.search(username)
        if (existing.isEmpty()) {
            val user = UserRepresentation().apply {
                this.username = username
                this.email = email
                isEnabled = true
                isEmailVerified = true

                attributes = mutableMapOf()
                edxSession?.let {
                    attributes["edx_csrf_token"] = listOf(it.csrfToken)
                    attributes["edx_expires_at"] = listOf(it.expiresAt.toString())
                }

                attributes["roles"] = listOf("OPENEDX_USER")

                this.realmRoles = listOf("OPENEDX_USER")

                val credential = CredentialRepresentation().apply {
                    type = "password"
                    value = generateDeterministicPassword(username)
                    isTemporary = false
                }
                this.credentials = listOf(credential)
            }
            users.create(user)
        }
    }

    fun createUserAndGetId(
        username: String,
        email: Email,
        role: String,
        rawPassword: String,
        keycloak: Keycloak? = null,
    ): UUID {
        val master = keycloak ?: keycloakMaster

        val realm = master.realm(masterRealm)
        val users = realm.users()
        val existing = users.search(username)
        if (existing.isNotEmpty()) {
            throw UserAlreadyExistAuthenticationException("User $username already exists")
        }
        val user = UserRepresentation().apply {
            this.username = username
            this.email = email.value
            isEnabled = true
            isEmailVerified = true

            this.attributes = mutableMapOf("role" to listOf(role))
            this.realmRoles = listOf(role)

            val credential = CredentialRepresentation().apply {
                type = "password"
                value = rawPassword
                isTemporary = false
            }
            this.credentials = listOf(credential)
        }
        val createdUser = users.create(user)
        createdUser.location ?: throw BusinessLogicException("Auth server error")

        return UUID.fromString(createdUser.location.toString().split('/').last())
    }
//
//    fun loginOpenedxUser(request: LoginRequestDto): LoginResponseDto {
//        logger.info("Login attempt for user: ${request.username}")
//
//        val session = edxSessionAuthManager.authenticateUser(request.username, request.password)
//
//        val email = edxSessionAuthManager.getUserEmail(session, request.username)
//            ?: "placeholder_${request.username}@mail.com"
//
//        return transactionTemplate.execute {
//            val user = authenticateOrCreateOpenedxUser(request.username, Email(email))
//            keycloakService.createEdxUser(
//                username = request.username,
//                email = email,
//                edxSession = session,
//            )
//            val token = jwtTokenService.generateToken(
//                user.id!!,
//                user.username!!,
//                Email(user.email!!),
//                setOf("OPENEDX_USER"),
//                session,
//            )
//            LoginResponseDto(
//                accessToken = token,
//                tokenType = "Bearer",
//                expiresIn = session.expiresAt.epochSecond - Instant.now().epochSecond,
//                username = user.username!!,
//                role = "OPENEDX_USER",
//            )
//        }!!
//    }

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


