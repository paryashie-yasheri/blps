package soaringLizards.blps

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import soaringLizards.blps.integration.IntegrationPostgres
import soaringLizards.blps.integration.TestcontainersCleanupExtension

@SpringBootTest
@ExtendWith(TestcontainersCleanupExtension::class)
class ApplicationTests {


    companion object {
        @JvmStatic
        @DynamicPropertySource
        fun registerProperties(registry: DynamicPropertyRegistry) {
            IntegrationPostgres.registerProperties(registry)
        }
    }

    @Test
    fun contextLoads() {
    }
}
