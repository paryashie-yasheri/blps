package soaringLizards.blps

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import soaringLizards.blps.integration.DegradedRewardsPostgres
import soaringLizards.blps.integration.TestcontainersCleanupExtension

@SpringBootTest
@AutoConfigureMockMvc
@ExtendWith(TestcontainersCleanupExtension::class)
class DegradedDatabaseIntegrationTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    companion object {
        @JvmStatic
        @DynamicPropertySource
        fun registerProperties(registry: DynamicPropertyRegistry) {
            DegradedRewardsPostgres.registerProperties(registry)
        }
    }

    @Test
    fun `rewards database outage returns service unavailable without leaking internals`() {
        mockMvc.perform(
            get("/api/student/rewards")
                .with(user("student").roles("OPENEDX_USER")),
        )
            .andExpect(status().isServiceUnavailable)
            .andExpect(jsonPath("$.status").value(503))
            .andExpect(jsonPath("$.error").value("Service Unavailable"))
            .andExpect(jsonPath("$.message").value("Required database is unavailable"))
            .andExpect(jsonPath("$.details").doesNotExist())
            .andExpect(jsonPath("$.trace").doesNotExist())
    }
}
