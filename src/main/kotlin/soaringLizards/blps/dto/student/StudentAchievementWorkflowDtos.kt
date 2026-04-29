package soaringLizards.blps.dto.student

import soaringLizards.blps.dto.edx.EdxEnrollmentResponse
import soaringLizards.blps.service.AwardedAchievement

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

fun List<AwardedAchievement>.toAwardedAchievementDtos() =
    map {
        AwardedAchievementDto(
            achievementTypeId = it.achievementTypeId.toString(),
            code = it.code,
            name = it.name,
            pointsAwarded = it.pointsAwarded,
        )
    }
