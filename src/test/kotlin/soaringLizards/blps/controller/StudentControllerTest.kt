package soaringLizards.blps.controller

import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import soaringLizards.blps.config.userDetails.JwtUserDetails
import soaringLizards.blps.dto.edx.EdxAuthenticatedSession
import soaringLizards.blps.dto.student.CourseEnrollResponse
import soaringLizards.blps.dto.student.LessonCompleteResponse
import soaringLizards.blps.dto.user.Email
import org.camunda.bpm.engine.RuntimeService
import soaringLizards.blps.service.AchievementService
import soaringLizards.blps.service.EdxService
import soaringLizards.blps.service.RewardService
import soaringLizards.blps.service.StudentAchievementWorkflowService
import soaringLizards.blps.service.UserBalanceService
import java.time.Instant
import java.util.UUID

class StudentControllerTest {
    private val edxService = mock<EdxService>()
    private val userBalanceService = mock<UserBalanceService>()
    private val achievementService = mock<AchievementService>()
    private val rewardService = mock<RewardService>()
    private val studentAchievementWorkflowService = mock<StudentAchievementWorkflowService>()

    private val runtimeService = mock<RuntimeService>()

    private val controller = StudentController(
        edxService = edxService,
        studentAchievementWorkflowService = studentAchievementWorkflowService,
        userBalanceService = userBalanceService,
        achievementService = achievementService,
        rewardService = rewardService,
        runtimeService = runtimeService,
    )

    @Test
    fun `completeLesson delegates to workflow service`() {
        val user = testUser()
        val courseKey = "course-v1:ITMO+Kotlin"
        val blockId = "block-v1:ITMO+type@sequential+block@intro"
        whenever(studentAchievementWorkflowService.completeLesson(user, courseKey, blockId))
            .thenReturn(
                LessonCompleteResponse(
                    awardedAchievements = emptyList(),
                    currentBalance = 100,
                ),
            )

        controller.completeLesson(user, courseKey, blockId)

        verify(studentAchievementWorkflowService).completeLesson(user, courseKey, blockId)
        verify(edxService, org.mockito.kotlin.never()).trackCompletion(any(), any())
    }

    @Test
    fun `enroll delegates to workflow service`() {
        val user = testUser()
        val courseId = "course-v1:ITMO+Kotlin"
        whenever(studentAchievementWorkflowService.enroll(user, courseId))
            .thenReturn(
                CourseEnrollResponse(
                    enrollment = null,
                    awardedAchievements = emptyList(),
                    currentBalance = 50,
                ),
            )

        controller.enroll(user, courseId)

        verify(studentAchievementWorkflowService).enroll(user, courseId)
        verify(achievementService, org.mockito.kotlin.never()).checkAndAwardAchievements(eq(user.id), any())
    }

    private fun testUser() =
        JwtUserDetails(
            id = UUID.randomUUID(),
            _username = "student",
            email = Email("student@example.com"),
            role = "OPENEDX_USER",
            token = "token",
            edxSession = EdxAuthenticatedSession(
                cookieHeader = "sessionid=test",
                csrfToken = "csrf",
                expiresAt = Instant.now().plusSeconds(3600),
            ),
        )
}
