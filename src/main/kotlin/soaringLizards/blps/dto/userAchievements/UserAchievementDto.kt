package soaringLizards.blps.dto.userAchievements

import java.util.*
import java.time.OffsetDateTime

data class UserAchievementDto(
    val balanceId: UUID,
    val achievementTypeId: UUID,
    val earnedAt: OffsetDateTime,
    val referenceId: UUID,
    val referenceType: String,
    val isClaimed: Boolean,
)
