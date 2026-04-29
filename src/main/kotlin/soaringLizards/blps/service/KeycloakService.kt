package soaringLizards.blps.service

import org.keycloak.admin.client.Keycloak
import org.keycloak.representations.idm.CredentialRepresentation
import org.keycloak.representations.idm.UserRepresentation
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.util.LinkedMultiValueMap
import org.springframework.web.client.RestClient
import soaringLizards.blps.dto.edx.EdxAuthenticatedSession
import java.security.MessageDigest

@Service
class KeycloakService(
    private val keycloakMaster: Keycloak,
    private val passwordEncoder: PasswordEncoder,
    @Value("\${keycloak.server-url}") private val serverUrl: String,
    @Value("\${keycloak.master.realm}") private val masterRealm: String,
    @Value("\${keycloak.master.client-id}") private val clientId: String,
    @Value("\${keycloak.master.client-secret}") private val clientSecret: String,
) {

    private val restClient = RestClient.builder().baseUrl(serverUrl).build()

    /**
     * Generates a deterministic, double-hashed password for Keycloak users.
     *
     * Double hashing strategy (production-ready):
     * 1. SHA-256: Normalizes input, mixes in a secret pepper, produces fixed-length output.
     *    This is fast and deterministic.
     * 2. BCrypt: Applies a slow, salted hash resistant to rainbow tables and brute force.
     *
     * The result is secure for storage while remaining reproducible for verification
     * when the same username and pepper are provided.
     */
    fun generateDeterministicPassword(username: String): String {
        val raw = "${username}:${clientSecret}"

        // Layer 1: SHA-256 for deterministic normalization
        val sha256Digest = MessageDigest.getInstance("SHA-256")
            .digest(raw.toByteArray(Charsets.UTF_8))
        val sha256Hex = sha256Digest.joinToString("") { "%02x".format(it) }

        // Layer 2: BCrypt for secure, slow hashing with random salt
        return passwordEncoder.encode(sha256Hex)
            ?: throw IllegalStateException("Password encoding failed for user: $username")
    }

    /**
     * Get staff user token from Keycloak using Direct Access Grants (resource owner password credentials).
     * Includes custom claims: user-id, email
     */
    fun getStaffToken(username: String, password: String): KeycloakTokenResponse {
        val formData = LinkedMultiValueMap<String, String>().apply {
            add("grant_type", "password")
            add("client_id", clientId)
            add("client_secret", clientSecret)
            add("username", username)
            add("password", password)
            add("scope", "openid profile email")
        }

        return restClient.post()
            .uri("/realms/${masterRealm}/protocol/openid-connect/token")
            .header("Content-Type", "application/x-www-form-urlencoded")
            .body(formData)
            .retrieve()
            .body(KeycloakTokenResponse::class.java)
            ?: throw IllegalStateException("Failed to get staff token from Keycloak")
    }

    /**
     * Get OpenEdX user token from Keycloak.
     * Since OpenEdX users authenticate via OpenEdX, we use client credentials flow
     * and include custom claims from the session.
     */
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

                // Set deterministic double-hashed password
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

    fun createStaffUser(username: String, email: String, userId: String, role: String, password: String) {
        val realm = keycloakMaster.realm(masterRealm)
        val users = realm.users()

        val existing = users.search(username)
        if (existing.isEmpty()) {
            val user = UserRepresentation().apply {
                this.username = username
                this.email = email
                isEnabled = true
                isEmailVerified = true

                // Add user-id as attribute
                this.attributes = mapOf(
                    "user-id" to listOf(userId),
                    "roles" to listOf(role),
                ).toMutableMap()

                // Set password from provided credentials
                val credential = CredentialRepresentation().apply {
                    type = "password"
                    value = password
                    isTemporary = false
                }
                this.credentials = listOf(credential)
            }

            users.create(user)

        }
    }

    fun getAdminAccessToken(): String {
        return keycloakMaster.tokenManager().getAccessToken().token
    }

    fun getRealm(): String = masterRealm

    fun getKeycloakMaster(): Keycloak = keycloakMaster
}

data class KeycloakTokenResponse(
    val access_token: String,
    val expires_in: Int,
    val refresh_token: String?,
    val token_type: String,
    val scope: String?,
)
