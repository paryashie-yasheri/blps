package soaringLizards.blps.controller

import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import soaringLizards.blps.config.userDetails.JwtUserDetails
import soaringLizards.blps.dto.balanceTransactions.BalanceResponse
import soaringLizards.blps.dto.edx.EdxAuthenticatedSession
import soaringLizards.blps.dto.edx.EdxCompletionBatchResponse
import soaringLizards.blps.dto.edx.EdxCourseProgressResponse
import soaringLizards.blps.dto.edx.EdxSubsectionCompletionResponse
import soaringLizards.blps.dto.user.Email
import soaringLizards.blps.entity.AchievementEventType
import soaringLizards.blps.service.AchievementContext
import soaringLizards.blps.service.AchievementService
import soaringLizards.blps.service.EdxService
import soaringLizards.blps.service.RewardService
import soaringLizards.blps.service.UserBalanceService
import soaringLizards.blps.service.UserService
import java.time.Instant
import java.util.UUID
import kotlin.test.assertEquals

class StudentControllerTest {
    private val edxService = mock<EdxService>()
    private val userBalanceService = mock<UserBalanceService>()
    private val achievementService = mock<AchievementService>()
    private val rewardService = mock<RewardService>()
    private val userService = mock<UserService>()

    private val controller = StudentController(
        edxService = edxService,
        userBalanceService = userBalanceService,
        achievementService = achievementService,
        rewardService = rewardService,
        userService = userService,
    )

    @Test
    fun `completeLesson emits derived achievement events from Open edX progress`() {
        val user = testUser()
        val courseKey = "course-v1:ITMO+Kotlin"
        val blockId = "block-v1:ITMO+type@sequential+block@intro"

        whenever(edxService.trackCompletion(eq(user), any())).thenReturn(EdxCompletionBatchResponse())
        whenever(edxService.getSubsectionCompletion(user, courseKey, blockId))
            .thenReturn(EdxSubsectionCompletionResponse(completion = 1.0))
        whenever(edxService.getProgress(user, courseKey)).thenReturn(
            EdxCourseProgressResponse(
                completionSummary = mapOf(
                    "complete_count" to 3,
                    "incomplete_count" to 0,
                ),
                userHasPassingGrade = false,
            ),
        )
        whenever(achievementService.checkAndAwardAchievements(eq(user.id), any())).thenReturn(emptyList())
        whenever(userBalanceService.getCurrentUserBalance(user.id)).thenReturn(
            BalanceResponse(
                balance = 100,
                currentBalance = 100,
                totalEarned = 100,
                totalSpent = 0,
            ),
        )

        val response = controller.completeLesson(user, courseKey, blockId)

        val contextCaptor = argumentCaptor<AchievementContext>()
        verify(achievementService, times(4)).checkAndAwardAchievements(eq(user.id), contextCaptor.capture())
        assertEquals(
            listOf(
                AchievementEventType.LESSON_COMPLETE,
                AchievementEventType.SUBSECTION_COMPLETE,
                AchievementEventType.COURSE_PROGRESS_UPDATED,
                AchievementEventType.COURSE_COMPLETE,
            ),
            contextCaptor.allValues.map { it.eventType },
        )
        assertEquals(1.0, contextCaptor.allValues.single { it.eventType == AchievementEventType.COURSE_COMPLETE }.completion)
        assertEquals(100, response.currentBalance)
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
