package soaringLizards.blps.entity

import soaringLizards.blps.jooq.generated.tables.records.UserAchievementsRecord
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

fun UserAchievementsRecord.toEntity() =
    UserAchievement(
        userAchievementId = this.userAchievementId,
        balanceId = this.balanceId,
        achievementTypeId = this.achievementTypeId,
        earnedAt = this.earnedAt,
        referenceId = this.referenceId,
        referenceType = this.referenceType,
        isClaimed = this.isClaimed,
    )
