package soaringLizards.blps.entity

import java.util.*

data class AchievementType(
    val achievementTypeId: UUID?,
    val code: String?,
    val name: String?,
    val description: String?,
    val pointsAwarded: Int?,
    val iconKey: String?,
    val isRepeatable: Boolean?,
    val isActive: Boolean?,
    val conditions: List<AchievementCondition> = emptyList(),
)
