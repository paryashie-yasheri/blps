package soaringLizards.blps.service

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class KeycloakServiceWorkflowMockedTest {

    private val keycloakMaster: org.keycloak.admin.client.Keycloak = mock()
    private val userBalanceService: UserBalanceService = mock()
    private val edxSessionAuthManager: soaringLizards.blps.config.EdxSessionAuthManager = mock()
    private val userRepository: soaringLizards.blps.repository.UserRepository = mock()
    private val userBalanceRepository: soaringLizards.blps.repository.UserBalanceRepository = mock()
    private val transactionTemplate: org.springframework.transaction.support.TransactionTemplate = mock()

    private lateinit var keycloakService: KeycloakService

    @BeforeEach
    fun setup() {
        keycloakService = KeycloakService(
            keycloakMaster = keycloakMaster,
            userBalanceService = userBalanceService,
            edxSessionAuthManager = edxSessionAuthManager,
            transactionTemplate = transactionTemplate,
            userRepository = userRepository,
            serverUrl = "http://localhost:8081",
            masterRealm = "blps-master123",
            clientId = "blps-client321",
            clientSecret = "test-secret",
        )
    }

    @Test
    fun `generateDeterministicPassword creates consistent password for same user`() {
        val password1 = keycloakService.generateDeterministicPassword("user1")
        val password2 = keycloakService.generateDeterministicPassword("user1")
        val password3 = keycloakService.generateDeterministicPassword("user2")

        assertEquals(password1, password2)
        assertTrue(password1 != password3)
    }

    @Test
    fun `generateDeterministicPassword produces valid SHA256 hex`() {
        val password = keycloakService.generateDeterministicPassword("testuser")

        assertEquals(64, password.length)
        password.forEach { char ->
            assertTrue(char.isDigit() || char in 'a'..'f')
        }
    }

    @Test
    fun `getRealm returns configured master realm`() {
        assertEquals("blps-master123", keycloakService.getRealm())
    }

    @Test
    fun `getKeycloakMaster returns keycloak instance`() {
        assertEquals(keycloakMaster, keycloakService.getKeycloakMaster())
    }
}