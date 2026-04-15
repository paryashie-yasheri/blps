package soaringLizards.blps.dto.userAchievements

import java.util.*

data class UserAchievementDto(
    val userAchievementId: UUID? = null,
    val balanceId: UUID,
    val achievementTypeId: UUID,
    val earnedAt: java.time.OffsetDateTime? = null,
    val referenceId: UUID? = null,
    val referenceType: String? = null,
    val isClaimed: Boolean = false,
    val metadata: String? = null,
)

data class UserAchievementWithDetailsDto(
    val userAchievementId: String?,
    val achievementTypeId: String?,
    val achievementCode: String?,
    val achievementName: String?,
    val achievementDescription: String?,
    val pointsAwarded: Int?,
    val icon: String?,
    val earnedAt: String?,
    val isClaimed: Boolean?,
    val referenceId: String?,
    val referenceType: String?,
)