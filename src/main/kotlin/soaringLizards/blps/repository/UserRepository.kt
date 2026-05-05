package soaringLizards.blps.repository

import jakarta.ws.rs.core.Response
import org.keycloak.admin.client.CreatedResponseUtil
import org.keycloak.admin.client.Keycloak
import org.keycloak.representations.idm.CredentialRepresentation
import org.keycloak.representations.idm.UserRepresentation
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Repository
import soaringLizards.blps.dto.edx.EdxAuthenticatedSession
import soaringLizards.blps.dto.user.Email
import soaringLizards.blps.exception.BusinessLogicException
import java.util.*

private val logger = LoggerFactory.getLogger(UserRepository::class.java)

@Repository
class UserRepository(
    private val keycloakMaster: Keycloak,
    @Value("\${keycloak.master.realm}") private val masterRealm: String,
) {

    fun userExists(username: String): UUID? {
        val realm = keycloakMaster.realm(masterRealm)
        val users = realm.users()
        val existing = users.search(username, 0, 1)
        val stringId = existing.firstOrNull()?.id
        stringId ?: return null
        return UUID.fromString(stringId)
    }

    fun createUser(
        username: String,
        email: Email,
        role: String,
        rawPassword: String?,
        edxSession: EdxAuthenticatedSession?,
    ): UUID {
        val realm = keycloakMaster.realm(masterRealm)
        val users = realm.users()

        val user = UserRepresentation().apply {
            this.username = username
            this.email = email.value
            isEnabled = true
            isEmailVerified = true

            rawPassword?.let {
                val credential = CredentialRepresentation().apply {
                    type = "password"
                    value = rawPassword
                    isTemporary = false
                }
                this.credentials = listOf(credential)
            }

            this.realmRoles = listOf(role)

            this.attributes = mutableMapOf(
                "role" to listOf(role),
                "edxCookieHeader" to listOf(edxSession?.cookieHeader ?: "staff"),
                "edxCsrfToken" to listOf(edxSession?.csrfToken ?: "staff"),
            )
        }
        val createdUser = users.create(user)

        println(createdUser)

        if (logger.isDebugEnabled) {
            logger.debug(
                "Keycloak create response - Status: {}, Location: {}, Headers: {}",
                createdUser.status,
                createdUser.location,
                createdUser.headers.mapValues { it.value.joinToString(",") },
            )
        }

        when {
            createdUser.status == Response.Status.CREATED.statusCode -> {
                val userId = CreatedResponseUtil.getCreatedId(createdUser)
                logger.info("User created successfully: {}", userId)
                return UUID.fromString(userId)
            }

            createdUser.status >= 400 -> {
                val errorBody = try {
                    createdUser.readEntity(String::class.java)
                } catch (e: Exception) {
                    "Unable to read error body: ${e.message}"
                }
                logger.error("Keycloak error {}: {}", createdUser.status, errorBody)
                throw BusinessLogicException("Auth server error (${createdUser.status}): $errorBody")
            }

            else -> {
                logger.warn("Unexpected Keycloak response: {}", createdUser.status)
                throw BusinessLogicException("Unexpected auth server response: ${createdUser.status}")
            }
        }
    }
}
