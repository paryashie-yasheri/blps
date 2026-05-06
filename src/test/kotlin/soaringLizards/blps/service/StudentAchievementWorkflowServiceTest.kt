package soaringLizards.blps.service

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
import soaringLizards.blps.dto.edx.EdxEnrollmentResponse
import soaringLizards.blps.dto.edx.EdxSubsectionCompletionResponse
import soaringLizards.blps.dto.user.Email
import soaringLizards.blps.entity.AchievementEventType
import java.time.Instant
import java.util.UUID
import kotlin.test.assertEquals

class StudentAchievementWorkflowServiceTest {
    private val edxService = mock<EdxService>()
    private val achievementService = mock<AchievementService>()
    private val userBalanceService = mock<UserBalanceService>()

    private val service = StudentAchievementWorkflowService(
        edxService = edxService,
        achievementService = achievementService,
        userBalanceService = userBalanceService,
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

        val response = service.completeLesson(user, courseKey, blockId)

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

    @Test
    fun `enroll calls Open edX and awards course_enrolled event`() {
        val user = testUser()
        val courseId = "course-v1:ITMO+Kotlin"
        val achievementTypeId = UUID.randomUUID()

        whenever(edxService.enroll(eq(user), any())).thenReturn(EdxEnrollmentResponse(mode = "audit"))
        whenever(achievementService.checkAndAwardAchievements(eq(user.id), any())).thenReturn(
            listOf(
                AwardedAchievement(
                    achievementTypeId = achievementTypeId,
                    code = "course_joined",
                    name = "Course Joined",
                    pointsAwarded = 10,
                ),
            ),
        )
        whenever(userBalanceService.getCurrentUserBalance(user.id)).thenReturn(
            BalanceResponse(
                balance = 110,
                currentBalance = 110,
                totalEarned = 110,
                totalSpent = 0,
            ),
        )

        val response = service.enroll(user, courseId)

        val enrollRequestCaptor = argumentCaptor<soaringLizards.blps.dto.edx.EdxEnrollmentRequest>()
        verify(edxService).enroll(eq(user), enrollRequestCaptor.capture())
        assertEquals(courseId, enrollRequestCaptor.firstValue.courseId)
        assertEquals("audit", enrollRequestCaptor.firstValue.mode)
        assertEquals(user.username, enrollRequestCaptor.firstValue.user)

        val contextCaptor = argumentCaptor<AchievementContext>()
        verify(achievementService).checkAndAwardAchievements(eq(user.id), contextCaptor.capture())
        assertEquals(AchievementEventType.COURSE_ENROLLED, contextCaptor.firstValue.eventType)
        assertEquals(courseId, contextCaptor.firstValue.courseKey)
        assertEquals("course_joined", response.awardedAchievements.single().code)
        assertEquals(110, response.currentBalance)
    }

    @Test
    fun `percent value 1 is treated as one percent and does not trigger course_complete`() {
        val user = testUser()
        val courseKey = "course-v1:ITMO+Kotlin"
        val blockId = "block-v1:ITMO+type@sequential+block@intro"

        whenever(edxService.trackCompletion(eq(user), any())).thenReturn(EdxCompletionBatchResponse())
        whenever(edxService.getSubsectionCompletion(user, courseKey, blockId))
            .thenReturn(EdxSubsectionCompletionResponse(completion = 0.0))
        whenever(edxService.getProgress(user, courseKey)).thenReturn(
            EdxCourseProgressResponse(
                completionSummary = mapOf(
                    "percent" to 1,
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

        service.completeLesson(user, courseKey, blockId)

        val contextCaptor = argumentCaptor<AchievementContext>()
        verify(achievementService, times(2)).checkAndAwardAchievements(eq(user.id), contextCaptor.capture())
        assertEquals(
            listOf(
                AchievementEventType.LESSON_COMPLETE,
                AchievementEventType.COURSE_PROGRESS_UPDATED,
            ),
            contextCaptor.allValues.map { it.eventType },
        )
        assertEquals(0.01, contextCaptor.allValues.single { it.eventType == AchievementEventType.COURSE_PROGRESS_UPDATED }.completion)
    }

    @Test
    fun `completeLesson awards achievements when completion is already recorded in Open edX`() {
        val user = testUser()
        val courseKey = "course-v1:ITMO+Kotlin"
        val blockId = "block-v1:ITMO+type@sequential+block@intro"

        whenever(edxService.trackCompletion(eq(user), any())).thenThrow(IllegalStateException("forbidden"))
        whenever(edxService.getSubsectionCompletion(user, courseKey, blockId))
            .thenReturn(EdxSubsectionCompletionResponse(completion = 1.0))
        whenever(edxService.getProgress(user, courseKey)).thenReturn(
            EdxCourseProgressResponse(
                completionSummary = mapOf("complete_count" to 1, "incomplete_count" to 0),
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

        service.completeLesson(user, courseKey, blockId)

        val contextCaptor = argumentCaptor<AchievementContext>()
        verify(achievementService, times(4)).checkAndAwardAchievements(eq(user.id), contextCaptor.capture())
        assertEquals(AchievementEventType.LESSON_COMPLETE, contextCaptor.firstValue.eventType)
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
