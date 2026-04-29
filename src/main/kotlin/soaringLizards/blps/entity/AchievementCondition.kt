package soaringLizards.blps.entity

import java.util.UUID

data class AchievementCondition(
    val conditionId: UUID?,
    val achievementTypeId: UUID?,
    val eventType: AchievementEventType,
    val coursePattern: String?,
    val blockPattern: String?,
    val minCompletion: Double?,
)
