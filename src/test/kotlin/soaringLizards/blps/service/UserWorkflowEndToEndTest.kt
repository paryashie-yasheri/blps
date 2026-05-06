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

@ExtendWith(SpringExtension::class, TestcontainersCleanupExtension::class)
@SpringBootTest
@TestPropertySource(properties = [
    "edx.bypass.enabled=true",
    "edx.bypass.expires-seconds=3600"
])
class UserWorkflowEndToEndTest {

    @Autowired
    private lateinit var keycloakService: KeycloakService

    @Autowired
    private lateinit var userBalanceService: UserBalanceService

    @Autowired
    private lateinit var edxService: EdxService

    @Autowired
    private lateinit var achievementService: AchievementService

    @Autowired
    private lateinit var rewardService: RewardService

    companion object {
        @JvmStatic
        @DynamicPropertySource
        fun dynamicProperties(registry: DynamicPropertyRegistry) {
            IntegrationPostgres.registerProperties(registry)
            IntegrationKeycloak.registerProperties(registry)
        }
    }

    @Test
    fun `full user workflow via services`() {
        val username = "huji_workflow_${System.currentTimeMillis()}"
        val password = "testpass123"

        val loginResponse = keycloakService.loginOpenedxUser(
            LoginRequestDto(username = username, password = password)
        )

        assertThat(loginResponse.accessToken).isNotBlank()
        assertThat(loginResponse.username).isEqualTo(username)
        assertThat(loginResponse.role).isEqualTo("OPENEDX_USER")

        val rewards = rewardService.getAvailableRewards()
        assertThat(rewards).isNotNull()
        assertThat(rewards.size).isGreaterThanOrEqualTo(0)
    }
}