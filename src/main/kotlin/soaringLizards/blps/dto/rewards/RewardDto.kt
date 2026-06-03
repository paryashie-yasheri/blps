package soaringLizards.blps.dto.rewards

data class RewardDto(
    val rewardId: String?,
    val name: String,
    val description: String?,
    val pointsCost: Int,
    val iconUrl: String?,
    val isActive: Boolean,
    val quantity: Int?,
) : java.io.Serializable

data class RewardCreateRequest(
    val name: String,
    val description: String?,
    val pointsCost: Int,
    val iconKey: String?,
    val isActive: Boolean = true,
    val quantity: Int?,
)

data class RewardUpdateRequest(
    val name: String?,
    val description: String?,
    val pointsCost: Int?,
    val iconKey: String?,
    val isActive: Boolean?,
    val quantity: Int?,
)