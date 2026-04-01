package soaringLizards.blps.dto.achievementTypes

data class AchievementTypeDto(
    val code: String,
    val name: String,
    val description: String,
    val pointsAwarded: Int,
    val icon: String,
    val isRepeatable: Boolean,
    val isActive: Boolean,
)
