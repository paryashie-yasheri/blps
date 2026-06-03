package soaringLizards.blps.controller

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Primary
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import soaringLizards.blps.integration.IntegrationPostgres
import soaringLizards.blps.integration.TestcontainersCleanupExtension
import soaringLizards.blps.service.S3Service

@SpringBootTest
@AutoConfigureMockMvc
@Import(S3ControllerIntegrationTest.TestBeans::class)
@ExtendWith(TestcontainersCleanupExtension::class)
class S3ControllerIntegrationTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var s3Service: S3Service

    companion object {
        @JvmStatic
        @DynamicPropertySource
        fun registerProperties(registry: DynamicPropertyRegistry) {
            IntegrationPostgres.registerProperties(registry)
        }
    }

    @BeforeEach
    fun setUp() {
        reset(s3Service)
    }

    @Test
    fun `GET supports S3 keys with nested path segments`() {
        whenever(s3Service.get(any())).thenReturn("https://cdn.test/uploads/file.png")

        mockMvc.perform(
            get("/api/img/s3/uploads/550e8400-e29b-41d4-a716-446655440000-file.png")
                .with(user("student").roles("OPENEDX_USER")),
        )
            .andExpect(status().isOk)
            .andExpect(content().string("https://cdn.test/uploads/file.png"))

        verify(s3Service).get("uploads/550e8400-e29b-41d4-a716-446655440000-file.png")
    }

    @Test
    fun `DELETE supports S3 keys with nested path segments`() {
        mockMvc.perform(
            delete("/api/img/s3/achievements/550e8400-e29b-41d4-a716-446655440000-icon.png")
                .with(user("staff").roles("STAFF")),
        )
            .andExpect(status().isOk)

        verify(s3Service).delete("achievements/550e8400-e29b-41d4-a716-446655440000-icon.png")
    }

    @Test
    fun `DELETE is forbidden for student role`() {
        mockMvc.perform(
            delete("/api/img/s3/achievements/file.png")
                .with(user("student").roles("OPENEDX_USER")),
        )
            .andExpect(status().isForbidden)
    }

    @org.springframework.boot.test.context.TestConfiguration
    class TestBeans {
        @Bean
        @Primary
        fun testS3Service(): S3Service = mock()
    }
}
