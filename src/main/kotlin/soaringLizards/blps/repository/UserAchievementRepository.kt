package soaringLizards.blps.repository

import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import soaringLizards.blps.dto.userAchievements.UserAchievementDto
import soaringLizards.blps.dto.userAchievements.UserAchievementWithDetailsDto
import soaringLizards.blps.entity.UserAchievement
import soaringLizards.blps.jooq.generated.tables.records.UserAchievementsRecord
import soaringLizards.blps.jooq.generated.tables.references.ACHIEVEMENT_TYPES
import soaringLizards.blps.jooq.generated.tables.references.USER_ACHIEVEMENTS
import java.util.*

@Repository
class UserAchievementRepository(
    private val dsl: DSLContext,
) {
    fun findById(id: UUID): UserAchievement? =
        dsl.select(USER_ACHIEVEMENTS)
            .where(USER_ACHIEVEMENTS.USER_ACHIEVEMENT_ID.eq(id))
            .fetchOne()
            ?.into(UserAchievementsRecord::class.java)
            ?.let { mapToEntity(it) }

    fun findByBalanceId(balanceId: UUID): List<UserAchievementWithDetailsDto> {
        return dsl.select(
            USER_ACHIEVEMENTS.USER_ACHIEVEMENT_ID,
            USER_ACHIEVEMENTS.ACHIEVEMENT_TYPE_ID,
            ACHIEVEMENT_TYPES.CODE,
            ACHIEVEMENT_TYPES.NAME,
            ACHIEVEMENT_TYPES.DESCRIPTION,
            ACHIEVEMENT_TYPES.POINTS_AWARDED,
            ACHIEVEMENT_TYPES.ICON,
            USER_ACHIEVEMENTS.EARNED_AT,
            USER_ACHIEVEMENTS.IS_CLAIMED,
            USER_ACHIEVEMENTS.REFERENCE_ID,
            USER_ACHIEVEMENTS.REFERENCE_TYPE,
        )
            .from(USER_ACHIEVEMENTS)
            .join(ACHIEVEMENT_TYPES).on(USER_ACHIEVEMENTS.ACHIEVEMENT_TYPE_ID.eq(ACHIEVEMENT_TYPES.ACHIEVEMENT_TYPE_ID))
            .where(USER_ACHIEVEMENTS.BALANCE_ID.eq(balanceId))
            .fetch { record ->
                UserAchievementWithDetailsDto(
                    userAchievementId = record.get(USER_ACHIEVEMENTS.USER_ACHIEVEMENT_ID)?.toString(),
                    achievementTypeId = record.get(USER_ACHIEVEMENTS.ACHIEVEMENT_TYPE_ID)?.toString(),
                    achievementCode = record.get(ACHIEVEMENT_TYPES.CODE),
                    achievementName = record.get(ACHIEVEMENT_TYPES.NAME),
                    achievementDescription = record.get(ACHIEVEMENT_TYPES.DESCRIPTION),
                    pointsAwarded = record.get(ACHIEVEMENT_TYPES.POINTS_AWARDED),
                    icon = record.get(ACHIEVEMENT_TYPES.ICON),
                    earnedAt = record.get(USER_ACHIEVEMENTS.EARNED_AT)?.toString(),
                    isClaimed = record.get(USER_ACHIEVEMENTS.IS_CLAIMED),
                    referenceId = record.get(USER_ACHIEVEMENTS.REFERENCE_ID)?.toString(),
                    referenceType = record.get(USER_ACHIEVEMENTS.REFERENCE_TYPE),
                )
            }
    }

    fun findByBalanceAndType(
        balanceId: UUID,
        achievementTypeId: UUID,
        referenceId: UUID?,
    ): List<UserAchievement> =
        dsl.selectFrom(USER_ACHIEVEMENTS)
            .where(USER_ACHIEVEMENTS.BALANCE_ID.eq(balanceId))
            .and(USER_ACHIEVEMENTS.ACHIEVEMENT_TYPE_ID.eq(achievementTypeId))
            .let { query ->
                if (referenceId != null) {
                    query.and(USER_ACHIEVEMENTS.REFERENCE_ID.eq(referenceId))
                } else {
                    query
                }
            }
            .fetch()
            .into(UserAchievementsRecord::class.java)
            .map { mapToEntity(it) }

    fun insert(achievementDto: UserAchievementDto): Int =
        dsl.insertInto(USER_ACHIEVEMENTS)
            .set(USER_ACHIEVEMENTS.BALANCE_ID, achievementDto.balanceId)
            .set(USER_ACHIEVEMENTS.ACHIEVEMENT_TYPE_ID, achievementDto.achievementTypeId)
            .set(USER_ACHIEVEMENTS.EARNED_AT, achievementDto.earnedAt)
            .set(USER_ACHIEVEMENTS.REFERENCE_ID, achievementDto.referenceId)
            .set(USER_ACHIEVEMENTS.REFERENCE_TYPE, achievementDto.referenceType)
            .set(USER_ACHIEVEMENTS.IS_CLAIMED, achievementDto.isClaimed)
            .execute()

    private fun mapToEntity(record: UserAchievementsRecord): UserAchievement {
        return UserAchievement(
            userAchievementId = record.userAchievementId,
            balanceId = record.balanceId,
            achievementTypeId = record.achievementTypeId,
            earnedAt = record.earnedAt,
            referenceId = record.referenceId,
            referenceType = record.referenceType,
            isClaimed = record.isClaimed,
        )
    }
}
