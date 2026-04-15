package soaringLizards.blps.repository

import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import soaringLizards.blps.dto.rewards.RewardCreateRequest
import soaringLizards.blps.dto.rewards.RewardUpdateRequest
import soaringLizards.blps.jooq.generated.tables.references.REWARDS
import java.util.*

@Repository
class RewardRepository(
    private val dsl: DSLContext,
) {
    fun findById(id: UUID) =
        dsl.selectFrom(REWARDS)
            .where(REWARDS.REWARD_ID.eq(id))
            .fetchOne()

    fun findAllActive(): List<soaringLizards.blps.jooq.generated.tables.records.RewardsRecord> =
        dsl.selectFrom(REWARDS)
            .where(REWARDS.IS_ACTIVE.eq(true))
            .fetch()

    fun findAll(): List<soaringLizards.blps.jooq.generated.tables.records.RewardsRecord> =
        dsl.selectFrom(REWARDS)
            .fetch()

    fun insert(request: RewardCreateRequest): Int =
        dsl.insertInto(REWARDS)
            .set(REWARDS.NAME, request.name)
            .set(REWARDS.DESCRIPTION, request.description)
            .set(REWARDS.POINTS_COST, request.pointsCost)
            .set(REWARDS.ICON_KEY, request.iconKey)
            .set(REWARDS.IS_ACTIVE, request.isActive)
            .set(REWARDS.QUANTITY, request.quantity)
            .execute()

    fun update(id: UUID, request: RewardUpdateRequest): Int {
        val query = dsl.update(REWARDS)
            .set(REWARDS.NAME, request.name ?: "")
            .set(REWARDS.DESCRIPTION, request.description)
            .set(REWARDS.POINTS_COST, request.pointsCost ?: 0)
            .set(REWARDS.ICON_KEY, request.iconKey)
            .set(REWARDS.IS_ACTIVE, request.isActive ?: true)
            .set(REWARDS.QUANTITY, request.quantity)
        
        return query.where(REWARDS.REWARD_ID.eq(id)).execute()
    }

    fun decrementQuantity(id: UUID): Int =
        dsl.update(REWARDS)
            .set(REWARDS.QUANTITY, REWARDS.QUANTITY.minus(1))
            .where(REWARDS.REWARD_ID.eq(id))
            .and(REWARDS.QUANTITY.isNotNull)
            .execute()

    fun delete(id: UUID) =
        dsl.deleteFrom(REWARDS)
            .where(REWARDS.REWARD_ID.eq(id))
            .execute()
}