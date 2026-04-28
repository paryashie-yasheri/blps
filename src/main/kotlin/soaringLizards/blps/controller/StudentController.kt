package soaringLizards.blps.controller

import io.swagger.v3.oas.annotations.tags.Tag
import org.slf4j.LoggerFactory
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import soaringLizards.blps.config.userDetails.JwtUserDetails
import soaringLizards.blps.dto.edx.EdxCompletionBatchRequest
import soaringLizards.blps.dto.edx.EdxCourseProgressResponse
import soaringLizards.blps.dto.edx.EdxEnrollmentRequest
import soaringLizards.blps.dto.edx.EdxEnrollmentResponse
import soaringLizards.blps.dto.userAchievements.UserAchievementWithDetailsDto
import soaringLizards.blps.entity.AchievementEventType
import soaringLizards.blps.service.*
import java.util.*

private val logger = LoggerFactory.getLogger(StudentController::class.java)
private const val COMPLETE_THRESHOLD = 1.0

@Tag(name = "Студент - управление курсами, достижениями и наградами")
@RestController
@RequestMapping("/api/student")
class StudentController(
    private val edxService: EdxService,
    private val userBalanceService: UserBalanceService,
    private val achievementService: AchievementService,
    private val rewardService: RewardService,
    private val userService: UserService,
) {
    @PreAuthorize("hasRole('OPENEDX_USER')")
    @GetMapping("/courses")
    fun getCourses(
        @AuthenticationPrincipal currentUser: JwtUserDetails,
        @RequestParam(required = false) page: Int?,
        @RequestParam(required = false) pageSize: Int?,
    ) = edxService.getCourses(currentUser, page, pageSize)

    @PreAuthorize("hasRole('OPENEDX_USER')")
    @GetMapping("/courses/{courseKey}")
    fun getCourse(@AuthenticationPrincipal currentUser: JwtUserDetails, @PathVariable courseKey: String) =
        edxService.getCourse(currentUser, courseKey)

    @PreAuthorize("hasRole('OPENEDX_USER')")
    @GetMapping("/courses/{courseKey}/metadata")
    fun getCourseMetadata(@AuthenticationPrincipal currentUser: JwtUserDetails, @PathVariable courseKey: String) =
        edxService.getCourseMetadata(currentUser, courseKey)

    @PreAuthorize("hasRole('OPENEDX_USER')")
    @GetMapping("/courses/{courseKey}/outline")
    fun getCourseOutline(@AuthenticationPrincipal currentUser: JwtUserDetails, @PathVariable courseKey: String) =
        edxService.getCourseOutline(currentUser, courseKey)

    @PreAuthorize("hasRole('OPENEDX_USER')")
    @PostMapping("/courses/{courseId}/enroll")
    fun enroll(
        @AuthenticationPrincipal currentUser: JwtUserDetails,
        @PathVariable courseId: String,
    ): CourseEnrollResponse {
        val enrollment = edxService.enroll(
            currentUser,
            EdxEnrollmentRequest(
                courseId = courseId,
                courseDetails = null,
                mode = "audit",
                user = currentUser.username,
            ),
        )

        val awardedAchievements = achievementService.checkAndAwardAchievements(
            currentUser.id,
            AchievementContext(
                eventType = AchievementEventType.COURSE_ENROLLED,
                courseKey = courseId,
                blockId = null,
                completion = null,
            ),
        )

        val newBalance = userBalanceService.getCurrentUserBalance(currentUser.id)

        return CourseEnrollResponse(
            enrollment = enrollment,
            awardedAchievements = awardedAchievements.toAwardedAchievementDtos(),
            currentBalance = newBalance.balance,
        )
    }

    @PreAuthorize("hasRole('OPENEDX_USER')")
    @PostMapping("/lessons/{courseKey}/{blockId}/complete")
    fun completeLesson(
        @AuthenticationPrincipal currentUser: JwtUserDetails,
        @PathVariable courseKey: String,
        @PathVariable blockId: String,
    ): LessonCompleteResponse {
        edxService.trackCompletion(
            currentUser,
            EdxCompletionBatchRequest(
                username = currentUser.username,
                courseKey = courseKey,
                blocks = mapOf(blockId to 1.0),
            ),
        )

        val awardedAchievements = awardAchievements(
            currentUser.id,
            buildLessonCompletionAchievementContexts(currentUser, courseKey, blockId),
        )

        val newBalance = userBalanceService.getCurrentUserBalance(currentUser.id)

        return LessonCompleteResponse(
            awardedAchievements = awardedAchievements.toAwardedAchievementDtos(),
            currentBalance = newBalance.balance,
        )
    }

    @PreAuthorize("hasRole('OPENEDX_USER')")
    @GetMapping("/balance")
    fun getBalance(@AuthenticationPrincipal currentUser: JwtUserDetails) =
        userBalanceService.getCurrentUserBalance(currentUser.id)

    @PreAuthorize("hasRole('OPENEDX_USER')")
    @GetMapping("/achievements")
    fun getAchievements(@AuthenticationPrincipal currentUser: JwtUserDetails): List<UserAchievementWithDetailsDto> =
        achievementService.getUserAchievements(currentUser.id)

    @PreAuthorize("hasRole('OPENEDX_USER')")
    @GetMapping("/rewards")
    fun getAvailableRewards() = rewardService.getAvailableRewards()

    @PreAuthorize("hasRole('OPENEDX_USER')")
    @GetMapping("/rewards/my")
    fun getMyRewards(@AuthenticationPrincipal currentUser: JwtUserDetails) =
        rewardService.getUserRewards(currentUser.id)

    @PreAuthorize("hasRole('OPENEDX_USER')")
    @PostMapping("/rewards/{rewardId}/redeem")
    fun redeemReward(
        @AuthenticationPrincipal currentUser: JwtUserDetails,
        @PathVariable rewardId: UUID,
    ) = rewardService.redeemReward(currentUser.id, rewardId)

    private fun awardAchievements(
        userId: UUID,
        contexts: List<AchievementContext>,
    ) = contexts.flatMap { context ->
        achievementService.checkAndAwardAchievements(userId, context)
    }

    private fun buildLessonCompletionAchievementContexts(
        currentUser: JwtUserDetails,
        courseKey: String,
        blockId: String,
    ): List<AchievementContext> {
        val contexts = mutableListOf(
            AchievementContext(
                eventType = AchievementEventType.LESSON_COMPLETE,
                courseKey = courseKey,
                blockId = blockId,
                completion = 1.0,
            ),
        )

        fetchSubsectionCompletion(currentUser, courseKey, blockId)
            ?.takeIf { it >= COMPLETE_THRESHOLD }
            ?.let { completion ->
                contexts += AchievementContext(
                    eventType = AchievementEventType.SUBSECTION_COMPLETE,
                    courseKey = courseKey,
                    blockId = blockId,
                    completion = completion,
                )
            }

        val courseProgress = fetchCourseProgress(currentUser, courseKey)
        if (courseProgress != null) {
            val courseCompletion = courseProgress.courseCompletion()
            contexts += AchievementContext(
                eventType = AchievementEventType.COURSE_PROGRESS_UPDATED,
                courseKey = courseKey,
                blockId = blockId,
                completion = courseCompletion,
            )

            if (courseProgress.isCourseComplete(courseCompletion)) {
                contexts += AchievementContext(
                    eventType = AchievementEventType.COURSE_COMPLETE,
                    courseKey = courseKey,
                    blockId = blockId,
                    completion = courseCompletion ?: COMPLETE_THRESHOLD,
                )
            }
        }

        return contexts
    }

    private fun fetchSubsectionCompletion(
        currentUser: JwtUserDetails,
        courseKey: String,
        blockId: String,
    ) = runCatching {
        edxService.getSubsectionCompletion(currentUser, courseKey, blockId).completion
    }.onFailure { error ->
        logger.warn("Failed to fetch Open edX subsection completion for achievement events: ${error.message}")
    }.getOrNull()

    private fun fetchCourseProgress(
        currentUser: JwtUserDetails,
        courseKey: String,
    ) = runCatching {
        edxService.getProgress(currentUser, courseKey)
    }.onFailure { error ->
        logger.warn("Failed to fetch Open edX course progress for achievement events: ${error.message}")
    }.getOrNull()
}

data class CourseEnrollResponse(
    val enrollment: EdxEnrollmentResponse?,
    val awardedAchievements: List<AwardedAchievementDto>,
    val currentBalance: Int,
)

data class LessonCompleteResponse(
    val awardedAchievements: List<AwardedAchievementDto>,
    val currentBalance: Int,
)

data class AwardedAchievementDto(
    val achievementTypeId: String,
    val code: String,
    val name: String,
    val pointsAwarded: Int,
)

private fun List<AwardedAchievement>.toAwardedAchievementDtos() =
    map {
        AwardedAchievementDto(
            achievementTypeId = it.achievementTypeId.toString(),
            code = it.code,
            name = it.name,
            pointsAwarded = it.pointsAwarded,
        )
    }

private fun EdxCourseProgressResponse.courseCompletion(): Double? {
    val summary = completionSummary.orEmpty()

    return summary.numberValue(
        "completion",
        "completion_percent",
        "percent_complete",
        "percent",
        "progress",
    )?.toRatio()
        ?: summary.completionRatioFromCounts()
        ?: finalGrades?.toRatio()
}

private fun EdxCourseProgressResponse.isCourseComplete(courseCompletion: Double?) =
    userHasPassingGrade == true || courseCompletion?.let { it >= COMPLETE_THRESHOLD } == true

private fun Map<String, Any?>.completionRatioFromCounts(): Double? {
    val completed = numberValue("complete_count", "completed_count")
    val incomplete = numberValue("incomplete_count")
    val total = numberValue("total_count", "total")

    if (completed != null && incomplete != null && completed + incomplete > 0.0) {
        return completed / (completed + incomplete)
    }

    if (completed != null && total != null && total > 0.0) {
        return completed / total
    }

    return null
}

private fun Map<String, Any?>.numberValue(vararg keys: String): Double? =
    keys.firstNotNullOfOrNull { key -> this[key].toDoubleOrNull() }

private fun Any?.toDoubleOrNull(): Double? =
    when (this) {
        is Number -> toDouble()
        is String -> toDoubleOrNull()
        else -> null
    }

private fun Double.toRatio() =
    if (this > COMPLETE_THRESHOLD) this / 100.0 else this
