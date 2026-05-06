package soaringLizards.blps

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import soaringLizards.blps.entity.AchievementEventType
import soaringLizards.blps.integration.IntegrationPostgres
import soaringLizards.blps.integration.TestcontainersCleanupExtension
import soaringLizards.blps.service.AchievementTypeService

@SpringBootTest
@ExtendWith(TestcontainersCleanupExtension::class)
class AchievementSeedIntegrationTest {

    @Autowired
    private lateinit var achievementTypeService: AchievementTypeService

    companion object {
        @JvmStatic
        @DynamicPropertySource
        fun registerProperties(registry: DynamicPropertyRegistry) {
            IntegrationPostgres.registerProperties(registry)
        }
    }

    @Test
    fun `default achievement seeds are active and awardable`() {
        val achievementsByCode = achievementTypeService
            .getActiveAchievementTypesForAwarding()
            .associateBy { it.code }

        assertThat(achievementsByCode.keys)
            .contains("course_joined", "first_lesson", "course_completed")

        assertThat(achievementsByCode["course_joined"]?.conditions)
            .anySatisfy { condition ->
                assertThat(condition.eventType).isEqualTo(AchievementEventType.COURSE_ENROLLED)
            }

        assertThat(achievementsByCode["first_lesson"]?.conditions)
            .anySatisfy { condition ->
                assertThat(condition.eventType).isEqualTo(AchievementEventType.LESSON_COMPLETE)
                assertThat(condition.minCompletion).isEqualTo(1.0)
            }

        assertThat(achievementsByCode["course_completed"]?.conditions)
            .anySatisfy { condition ->
                assertThat(condition.eventType).isEqualTo(AchievementEventType.COURSE_COMPLETE)
                assertThat(condition.minCompletion).isEqualTo(1.0)
            }
    }
}
