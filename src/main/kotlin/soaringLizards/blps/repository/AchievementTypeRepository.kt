package soaringLizards.blps.repository

import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import soaringLizards.blps.dto.achievementTypes.AchievementTypeDto
import soaringLizards.blps.jooq.generated.tables.references.ACHIEVEMENT_TYPES
import java.util.*

@Repository
class AchievementTypeRepository(
    private val dsl: DSLContext,
) {
    fun findById(id: UUID) =
        dsl.select(ACHIEVEMENT_TYPES)
            .where(ACHIEVEMENT_TYPES.ACHIEVEMENT_TYPE_ID.eq(id))
            .fetchOne()

    fun findAllActive(): List<soaringLizards.blps.jooq.generated.tables.records.AchievementTypesRecord> =
        dsl.selectFrom(ACHIEVEMENT_TYPES)
            .where(ACHIEVEMENT_TYPES.IS_ACTIVE.eq(true))
            .fetch()

    fun findAll(): List<soaringLizards.blps.jooq.generated.tables.records.AchievementTypesRecord> =
        dsl.selectFrom(ACHIEVEMENT_TYPES)
            .fetch()

    fun insert(typeDto: AchievementTypeDto) =
        dsl.insertInto(ACHIEVEMENT_TYPES)
            .set(ACHIEVEMENT_TYPES.CODE, typeDto.code)
            .set(ACHIEVEMENT_TYPES.NAME, typeDto.name)
            .set(ACHIEVEMENT_TYPES.DESCRIPTION, typeDto.description)
            .set(ACHIEVEMENT_TYPES.POINTS_AWARDED, typeDto.pointsAwarded)
            .set(ACHIEVEMENT_TYPES.ICON, typeDto.icon)
            .set(ACHIEVEMENT_TYPES.IS_REPEATABLE, typeDto.isRepeatable)
            .set(ACHIEVEMENT_TYPES.IS_ACTIVE, typeDto.isActive)
            .execute()

    fun delete(typeId: UUID) =
        dsl.deleteFrom(ACHIEVEMENT_TYPES)
            .where(ACHIEVEMENT_TYPES.ACHIEVEMENT_TYPE_ID.eq(typeId))
            .execute()
}
