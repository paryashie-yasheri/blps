package soaringLizards.blps.repository

import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import soaringLizards.blps.dto.userAchievements.UserAchievementDto
import soaringLizards.blps.jooq.generated.tables.references.USER_ACHIEVEMENTS
import java.util.*

@Repository
class UserAchievementRepository(
    private val dsl: DSLContext,
) {
    fun findById(id: UUID) =
        dsl.select(USER_ACHIEVEMENTS)
            .where(USER_ACHIEVEMENTS.ACHIEVEMENT_TYPE_ID.eq(id))
            .fetchOne()

    fun insert(achievementDto: UserAchievementDto): Int =
        dsl.insertInto(USER_ACHIEVEMENTS)
            .set(USER_ACHIEVEMENTS.ACHIEVEMENT_TYPE_ID, achievementDto.achievementTypeId)
            .set(USER_ACHIEVEMENTS.BALANCE_ID, achievementDto.balanceId)
            .set(USER_ACHIEVEMENTS.EARNED_AT, achievementDto.earnedAt)
            .set(USER_ACHIEVEMENTS.REFERENCE_ID, achievementDto.referenceId)
            .set(USER_ACHIEVEMENTS.REFERENCE_TYPE, achievementDto.referenceType)
            .set(USER_ACHIEVEMENTS.IS_CLAIMED, achievementDto.isClaimed)
            .execute()
}
