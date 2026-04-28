package soaringLizards.blps.entity

import java.time.OffsetDateTime
import java.util.*

data class UserAchievement(
    val userAchievementId: UUID?,
    val balanceId: UUID?,
    val achievementTypeId: UUID?,
    val earnedAt: OffsetDateTime?,
    val referenceId: UUID?,
    val referenceType: String?,
    val isClaimed: Boolean?,
)
