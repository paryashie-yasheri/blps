package soaringLizards.blps.repository

import org.jooq.DSLContext
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Repository
import soaringLizards.blps.dto.rewards.UserRewardWithBalanceDto
import soaringLizards.blps.jooq.generated.tables.references.REWARDS
import soaringLizards.blps.jooq.generated.tables.references.USER_REWARDS
import java.util.*

@Repository
class UserRewardRepository(
    @Qualifier("rewardsDslContext")
    private val dsl: DSLContext,
) {
    fun findByBalanceId(balanceId: UUID): List<UserRewardWithBalanceDto> {
        return dsl.select(
            USER_REWARDS.USER_REWARD_ID,
            USER_REWARDS.REWARD_ID,
            REWARDS.NAME,
            REWARDS.DESCRIPTION,
            REWARDS.POINTS_COST,
            REWARDS.ICON_KEY,
            USER_REWARDS.REDEEMED_AT,
        )
            .from(USER_REWARDS)
            .join(REWARDS).on(USER_REWARDS.REWARD_ID.eq(REWARDS.REWARD_ID))
            .where(USER_REWARDS.BALANCE_ID.eq(balanceId))
            .fetch { record ->
                UserRewardWithBalanceDto(
                    userRewardId = record.get(USER_REWARDS.USER_REWARD_ID)?.toString(),
                    rewardId = record.get(USER_REWARDS.REWARD_ID)?.toString(),
                    rewardName = record.get(REWARDS.NAME),
                    rewardDescription = record.get(REWARDS.DESCRIPTION),
                    rewardPointsCost = record.get(REWARDS.POINTS_COST),
                    rewardIconUrl = record.get(REWARDS.ICON_KEY),
                    redeemedAt = record.get(USER_REWARDS.REDEEMED_AT)?.toString(),
                )
            }
    }

    fun insert(balanceId: UUID, rewardId: UUID): UUID =
        dsl.insertInto(USER_REWARDS)
            .set(USER_REWARDS.BALANCE_ID, balanceId)
            .set(USER_REWARDS.REWARD_ID, rewardId)
            .returningResult(USER_REWARDS.USER_REWARD_ID)
            .fetchOne(USER_REWARDS.USER_REWARD_ID)
            ?: throw IllegalStateException("Failed to insert user reward")

    fun delete(id: UUID) =
        dsl.deleteFrom(USER_REWARDS)
            .where(USER_REWARDS.USER_REWARD_ID.eq(id))
            .execute()

    fun deleteByBalanceId(balanceId: UUID) =
        dsl.deleteFrom(USER_REWARDS)
            .where(USER_REWARDS.BALANCE_ID.eq(balanceId))
            .execute()
}
