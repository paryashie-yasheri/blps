package soaringLizards.blps.dto.rewards

data class UserRewardDto(
    val userRewardId: String?,
    val reward: RewardDto,
    val redeemedAt: String,
) : java.io.Serializable

data class UserRewardWithBalanceDto(
    val userRewardId: String?,
    val rewardId: String?,
    val rewardName: String?,
    val rewardDescription: String?,
    val rewardPointsCost: Int?,
    val rewardIconUrl: String?,
    val redeemedAt: String?,
)