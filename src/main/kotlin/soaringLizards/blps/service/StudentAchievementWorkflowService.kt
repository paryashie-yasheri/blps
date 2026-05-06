package soaringLizards.blps.service

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import soaringLizards.blps.config.userDetails.JwtUserDetails
import soaringLizards.blps.dto.edx.EdxCompletionBatchRequest
import soaringLizards.blps.dto.edx.EdxCourseProgressResponse
import soaringLizards.blps.dto.edx.EdxEnrollmentRequest
import soaringLizards.blps.dto.student.CourseEnrollResponse
import soaringLizards.blps.dto.student.LessonCompleteResponse
import soaringLizards.blps.dto.student.toAwardedAchievementDtos
import soaringLizards.blps.entity.AchievementEventType
import java.util.*

private val logger = LoggerFactory.getLogger(StudentAchievementWorkflowService::class.java)
private const val COMPLETE_THRESHOLD = 1.0

@Service
class StudentAchievementWorkflowService(
    private val edxService: EdxService,
    private val achievementService: AchievementService,
    private val userBalanceService: UserBalanceService,
) {
    fun enroll(
        currentUser: JwtUserDetails,
        courseId: String,
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

    fun completeLesson(
        currentUser: JwtUserDetails,
        courseKey: String,
        blockId: String,
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

            if (courseProgress.isCourseComplete(courseCompletion))
                contexts += AchievementContext(
                    eventType = AchievementEventType.COURSE_COMPLETE,
                    courseKey = courseKey,
                    blockId = blockId,
                    completion = courseCompletion ?: COMPLETE_THRESHOLD,
                )
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

private fun EdxCourseProgressResponse.courseCompletion(): Double? {
    val summary = completionSummary.orEmpty()

    return summary.primaryCompletionRatio()
        ?: summary.completionRatioFromCounts()
        ?: finalGrades?.let(::normalizeGenericRatio)
}

private fun EdxCourseProgressResponse.isCourseComplete(courseCompletion: Double?) =
    userHasPassingGrade == true || courseCompletion?.let { it >= COMPLETE_THRESHOLD } == true

private fun Map<String, Any?>.completionRatioFromCounts(): Double? {
    val completed = firstNumericValue("complete_count", "completed_count")
    val incomplete = firstNumericValue("incomplete_count")
    val total = firstNumericValue("total_count", "total")

    if (completed != null && incomplete != null && completed + incomplete > 0.0)
        return completed / (completed + incomplete)

    if (completed != null && total != null && total > 0.0)
        return completed / total

    return null
}

private val PERCENT_KEYS = setOf("completion_percent", "percent_complete", "percent")
private val PRIMARY_COMPLETION_KEYS =
    listOf("completion", "completion_percent", "percent_complete", "percent", "progress")

private fun Map<String, Any?>.primaryCompletionRatio(): Double? =
    PRIMARY_COMPLETION_KEYS.firstNotNullOfOrNull { key ->
        parseRatioValue(this[key], key in PERCENT_KEYS)
    }

private fun Map<String, Any?>.firstNumericValue(vararg keys: String): Double? =
    keys.firstNotNullOfOrNull { key ->
        toNumericValue(this[key])
    }

private fun parseRatioValue(raw: Any?, percentField: Boolean): Double? {
    val numericValue = toNumericValue(raw) ?: return null

    return if (percentField) {
        normalizePercentRatio(raw, numericValue)
    } else {
        normalizeGenericRatio(numericValue)
    }
}

private fun normalizePercentRatio(raw: Any?, value: Double): Double =
    when {
        value > COMPLETE_THRESHOLD -> value / 100.0
        value == COMPLETE_THRESHOLD && isWholeNumber(raw) -> 0.01
        else -> value
    }

private fun normalizeGenericRatio(value: Double): Double =
    if (value > COMPLETE_THRESHOLD) value / 100.0 else value

private fun isWholeNumber(raw: Any?): Boolean =
    when (raw) {
        is Byte, is Short, is Int, is Long -> true
        is Float, is Double -> raw.toDouble() % 1.0 == 0.0
        is String -> raw.matches(Regex("^-?\\d+$"))
        else -> false
    }

private fun toNumericValue(raw: Any?): Double? =
    when (raw) {
        is Number -> raw.toDouble()
        is String -> raw.toDoubleOrNull()
        else -> null
    }
