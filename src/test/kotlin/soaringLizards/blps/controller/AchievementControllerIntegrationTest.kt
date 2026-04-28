package soaringLizards.blps.controller

import org.jooq.DSLContext
import org.jooq.impl.DSL
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
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Primary
import org.springframework.http.MediaType
import org.springframework.mock.web.MockMultipartFile
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import soaringLizards.blps.dto.achievementTypes.AchievementEventType
import soaringLizards.blps.dto.achievementTypes.AchievementConditionRequest
import soaringLizards.blps.dto.achievementTypes.AchievementTypeUpsertRequest
import soaringLizards.blps.entity.AchievementCondition
import soaringLizards.blps.entity.AchievementType
import soaringLizards.blps.integration.IntegrationPostgres
import soaringLizards.blps.integration.TestcontainersCleanupExtension
import soaringLizards.blps.repository.AchievementConditionRepository
import soaringLizards.blps.repository.AchievementTypeRepository
import soaringLizards.blps.service.S3Service
import tools.jackson.databind.ObjectMapper
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNull

@SpringBootTest
@AutoConfigureMockMvc
@Import(AchievementControllerIntegrationTest.TestBeans::class)
@ExtendWith(TestcontainersCleanupExtension::class)
class AchievementControllerIntegrationTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var achievementTypeRepository: AchievementTypeRepository

    @Autowired
    private lateinit var achievementConditionRepository: AchievementConditionRepository

    @Autowired
    private lateinit var dsl: DSLContext

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
        dsl.execute(
            """
            TRUNCATE TABLE user_achievements, balance_transactions, achievement_conditions, achievement_types,
            user_rewards, rewards, user_balances, users CASCADE
            """.trimIndent(),
        )
        reset(s3Service)
    }

    @Test
    fun `POST multipart creates achievement type with conditions and image`() {
        whenever(s3Service.uploadAchievementIcon(any())).thenReturn("achievements/first-course.png")
        whenever(s3Service.get("achievements/first-course.png")).thenReturn("https://cdn.test/achievements/first-course.png")

        val request = AchievementTypeUpsertRequest(
            code = "first_course",
            name = "First Course",
            description = "Awarded after the first course lesson",
            pointsAwarded = 50,
            isRepeatable = false,
            isActive = true,
            conditions = listOf(
                AchievementConditionRequest(
                    eventType = AchievementEventType.LESSON_COMPLETE,
                    coursePattern = "course-v1:ITMO+*",
                    blockPattern = "block-v1:*intro*",
                    minCompletion = 1.0,
                ),
                AchievementConditionRequest(
                    eventType = AchievementEventType.LESSON_COMPLETE,
                    coursePattern = "course-v1:ITMO+*",
                    minCompletion = 1.0,
                ),
            ),
        )

        mockMvc.perform(
            multipart("/api/achievements")
                .file(jsonPart(request))
                .file(imagePart("icon.png"))
                .with(user("staff").roles("STAFF")),
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.code").value("first_course"))
            .andExpect(jsonPath("$.iconKey").value("achievements/first-course.png"))
            .andExpect(jsonPath("$.iconUrl").value("https://cdn.test/achievements/first-course.png"))
            .andExpect(jsonPath("$.conditions.length()").value(2))

        val created = achievementTypeRepository.findAll().single()
        assertEquals("achievements/first-course.png", created.iconKey)
        assertEquals(2, countConditions(created.achievementTypeId!!))
    }

    @Test
    fun `PUT multipart replaces conditions and image`() {
        val existing = insertAchievementType(
            code = "lesson_master",
            iconKey = "achievements/old-icon.png",
            conditions = listOf(
                AchievementCondition(
                    conditionId = null,
                    achievementTypeId = null,
                    eventType = "lesson_complete",
                    coursePattern = "course-v1:OLD+*",
                    blockPattern = null,
                    minCompletion = 1.0,
                ),
                AchievementCondition(
                    conditionId = null,
                    achievementTypeId = null,
                    eventType = "lesson_complete",
                    coursePattern = "course-v1:OLD+*",
                    blockPattern = "block-v1:*old*",
                    minCompletion = 1.0,
                ),
            ),
        )

        whenever(s3Service.uploadAchievementIcon(any())).thenReturn("achievements/new-icon.png")
        whenever(s3Service.get("achievements/new-icon.png")).thenReturn("https://cdn.test/achievements/new-icon.png")

        val request = AchievementTypeUpsertRequest(
            code = "lesson_master",
            name = "Lesson Master Updated",
            description = "Updated achievement",
            pointsAwarded = 80,
            isRepeatable = true,
            isActive = true,
            conditions = listOf(
                AchievementConditionRequest(
                    eventType = AchievementEventType.LESSON_COMPLETE,
                    coursePattern = "course-v1:NEW+*",
                    blockPattern = "block-v1:*new*",
                    minCompletion = 0.9,
                ),
            ),
        )

        mockMvc.perform(
            multipart("/api/achievements/{id}", existing.achievementTypeId)
                .file(jsonPart(request))
                .file(imagePart("new-icon.png"))
                .with { servletRequest ->
                    servletRequest.method = "PUT"
                    servletRequest
                }
                .with(user("staff").roles("STAFF")),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.name").value("Lesson Master Updated"))
            .andExpect(jsonPath("$.iconKey").value("achievements/new-icon.png"))
            .andExpect(jsonPath("$.conditions.length()").value(1))

        val updated = achievementTypeRepository.findById(existing.achievementTypeId!!)!!
        assertEquals("achievements/new-icon.png", updated.iconKey)
        assertEquals(1, countConditions(existing.achievementTypeId!!))
        verify(s3Service).delete("achievements/old-icon.png")
    }

    @Test
    fun `DELETE removes achievement type and cascades conditions`() {
        val existing = insertAchievementType(
            code = "to_delete",
            iconKey = "achievements/delete-me.png",
            conditions = listOf(
                AchievementCondition(
                    conditionId = null,
                    achievementTypeId = null,
                    eventType = "lesson_complete",
                    coursePattern = "course-v1:DELETE+*",
                    blockPattern = null,
                    minCompletion = 1.0,
                ),
            ),
        )

        mockMvc.perform(
            delete("/api/achievements/{id}", existing.achievementTypeId)
                .with(user("staff").roles("STAFF")),
        )
            .andExpect(status().isNoContent)

        assertNull(achievementTypeRepository.findById(existing.achievementTypeId!!))
        assertEquals(0, countConditions(existing.achievementTypeId!!))
        verify(s3Service).delete("achievements/delete-me.png")
    }

    @Test
    fun `student receives forbidden on staff CRUD`() {
        val request = AchievementTypeUpsertRequest(
            code = "forbidden",
            name = "Forbidden",
            description = "Forbidden",
            pointsAwarded = 10,
            isRepeatable = false,
            isActive = true,
            conditions = listOf(
                AchievementConditionRequest(
                    eventType = AchievementEventType.LESSON_COMPLETE,
                    coursePattern = "course-v1:ITMO+*",
                    minCompletion = 1.0,
                ),
            ),
        )

        mockMvc.perform(
            multipart("/api/achievements")
                .file(jsonPart(request))
                .with(user("student").roles("OPENEDX_USER")),
        )
            .andExpect(status().isForbidden)
    }

    @Test
    fun `POST multipart returns bad request for unsupported event type`() {
        mockMvc.perform(
            multipart("/api/achievements")
                .file(
                    jsonPart(
                        """
                        {
                          "code": "invalid_event",
                          "name": "Invalid Event",
                          "description": "Invalid event type",
                          "pointsAwarded": 10,
                          "isRepeatable": false,
                          "isActive": true,
                          "conditions": [
                            {
                              "eventType": "course_complete",
                              "coursePattern": "course-v1:ITMO+*",
                              "minCompletion": 1.0
                            }
                          ]
                        }
                        """.trimIndent(),
                    ),
                )
                .with(user("staff").roles("STAFF")),
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error").value("Bad Request"))
    }

    private fun insertAchievementType(
        code: String,
        iconKey: String?,
        conditions: List<AchievementCondition>,
    ): AchievementType {
        val created = achievementTypeRepository.insert(
            AchievementType(
                achievementTypeId = null,
                code = code,
                name = code.replace('_', ' '),
                description = "seeded",
                pointsAwarded = 10,
                iconKey = iconKey,
                isRepeatable = false,
                isActive = true,
                conditions = emptyList(),
            ),
        )

        achievementConditionRepository.replaceForAchievementType(created.achievementTypeId!!, conditions)
        return created
    }

    private fun jsonPart(request: AchievementTypeUpsertRequest) =
        MockMultipartFile(
            "request",
            "",
            MediaType.APPLICATION_JSON_VALUE,
            objectMapper.writeValueAsBytes(request),
        )

    private fun jsonPart(request: String) =
        MockMultipartFile(
            "request",
            "",
            MediaType.APPLICATION_JSON_VALUE,
            request.toByteArray(),
        )

    private fun imagePart(filename: String) =
        MockMultipartFile(
            "image",
            filename,
            MediaType.IMAGE_PNG_VALUE,
            "png".toByteArray(),
        )

    private fun countConditions(achievementTypeId: UUID): Int =
        dsl.fetchCount(
            DSL.table(DSL.name("achievement_conditions")),
            DSL.field(DSL.name("achievement_type_id"), UUID::class.java).eq(achievementTypeId),
        )

    @TestConfiguration
    class TestBeans {
        @Bean
        @Primary
        fun testS3Service(): S3Service = mock()
    }
}
