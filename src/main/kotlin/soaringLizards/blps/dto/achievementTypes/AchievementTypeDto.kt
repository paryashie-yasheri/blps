package soaringLizards.blps.dto.achievementTypes

import jakarta.validation.Valid
import jakarta.validation.constraints.DecimalMax
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size

data class AchievementTypeUpsertRequest(
    @field:NotBlank
    val code: String,
    @field:NotBlank
    val name: String,
    val description: String,
    @field:Min(0)
    val pointsAwarded: Int,
    val isRepeatable: Boolean,
    val isActive: Boolean,
    @field:Valid
    @field:Size(min = 1)
    val conditions: List<AchievementConditionRequest>,
)

data class AchievementConditionRequest(
    @field:NotBlank
    @field:Pattern(regexp = "lesson_complete")
    val eventType: String,
    val coursePattern: String? = null,
    val blockPattern: String? = null,
    @field:DecimalMin("0.0")
    @field:DecimalMax("1.0")
    val minCompletion: Double? = null,
)

data class AchievementTypeResponse(
    val achievementTypeId: String,
    val code: String,
    val name: String,
    val description: String?,
    val pointsAwarded: Int,
    val iconKey: String?,
    val iconUrl: String?,
    val isRepeatable: Boolean,
    val isActive: Boolean,
    val conditions: List<AchievementConditionResponse>,
)

data class AchievementConditionResponse(
    val conditionId: String,
    val eventType: String,
    val coursePattern: String?,
    val blockPattern: String?,
    val minCompletion: Double?,
)
