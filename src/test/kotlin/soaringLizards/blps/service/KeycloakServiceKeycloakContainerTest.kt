package soaringLizards.blps.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.junit.jupiter.SpringExtension
import soaringLizards.blps.dto.user.LoginRequestDto
import soaringLizards.blps.integration.IntegrationKeycloak
import soaringLizards.blps.integration.IntegrationPostgres
import soaringLizards.blps.integration.TestcontainersCleanupExtension
import java.util.UUID

@ExtendWith(SpringExtension::class, TestcontainersCleanupExtension::class)
@SpringBootTest
@TestPropertySource(properties = [
    "edx.bypass.enabled=true",
    "edx.bypass.expires-seconds=3600"
])
class KeycloakServiceKeycloakContainerTest {

    @Autowired
    private lateinit var keycloakService: KeycloakService

    companion object {
        @JvmStatic
        @DynamicPropertySource
        fun dynamicProperties(registry: DynamicPropertyRegistry) {
            IntegrationPostgres.registerProperties(registry)
            IntegrationKeycloak.registerProperties(registry)
        }
    }

    @Test
    fun `getAdminAccessToken returns admin token`() {
        val adminToken = keycloakService.getAdminAccessToken()

        assertThat(adminToken).isNotBlank()
    }

    @Test
    fun `login via openedx creates user and returns token`() {
        val loginResponse = keycloakService.loginOpenedxUser(
            LoginRequestDto(
                username = "huji_test_${UUID.randomUUID()}",
                password = "testpass123",
            )
        )

        assertThat(loginResponse.accessToken).isNotBlank()
        assertThat(loginResponse.username).isNotBlank()
        assertThat(loginResponse.role).isEqualTo("OPENEDX_USER")
    }

    @Test
    fun `re-login existing user returns same username`() {
        val username = "huji_test_${UUID.randomUUID()}"

        val login1 = keycloakService.loginOpenedxUser(
            LoginRequestDto(username = username, password = "testpass123")
        )

        val login2 = keycloakService.loginOpenedxUser(
            LoginRequestDto(username = username, password = "testpass123")
        )

        assertThat(login1.username).isEqualTo(login2.username)
    }
}