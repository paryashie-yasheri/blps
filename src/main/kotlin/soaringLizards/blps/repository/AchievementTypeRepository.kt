package soaringLizards.blps.repository

import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import soaringLizards.blps.entity.AchievementType
import soaringLizards.blps.jooq.generated.tables.records.AchievementTypesRecord
import soaringLizards.blps.jooq.generated.tables.references.ACHIEVEMENT_TYPES
import java.util.UUID

@Repository
class AchievementTypeRepository(
    private val dsl: DSLContext,
) {
    fun findById(id: UUID): AchievementType? =
        dsl.selectFrom(ACHIEVEMENT_TYPES)
            .where(ACHIEVEMENT_TYPES.ACHIEVEMENT_TYPE_ID.eq(id))
            .fetchOne()
            ?.into(AchievementTypesRecord::class.java)
            ?.let { mapToEntity(it) }

    fun findAllActive(): List<AchievementType> =
        dsl.selectFrom(ACHIEVEMENT_TYPES)
            .where(ACHIEVEMENT_TYPES.IS_ACTIVE.eq(true))
            .orderBy(ACHIEVEMENT_TYPES.CODE.asc())
            .fetch()
            .into(AchievementTypesRecord::class.java)
            .map { mapToEntity(it) }

    fun findAll(): List<AchievementType> =
        dsl.selectFrom(ACHIEVEMENT_TYPES)
            .orderBy(ACHIEVEMENT_TYPES.CODE.asc())
            .fetch()
            .into(AchievementTypesRecord::class.java)
            .map { mapToEntity(it) }

    fun insert(type: AchievementType): AchievementType =
        dsl.insertInto(ACHIEVEMENT_TYPES)
            .set(ACHIEVEMENT_TYPES.CODE, type.code)
            .set(ACHIEVEMENT_TYPES.NAME, type.name)
            .set(ACHIEVEMENT_TYPES.DESCRIPTION, type.description)
            .set(ACHIEVEMENT_TYPES.POINTS_AWARDED, type.pointsAwarded)
            .set(ACHIEVEMENT_TYPES.ICON, type.iconKey)
            .set(ACHIEVEMENT_TYPES.IS_REPEATABLE, type.isRepeatable)
            .set(ACHIEVEMENT_TYPES.IS_ACTIVE, type.isActive)
            .returning()
            .fetchOne()
            ?.into(AchievementTypesRecord::class.java)
            ?.let { mapToEntity(it) }
            ?: throw IllegalStateException("Failed to insert achievement type")

    fun update(type: AchievementType): AchievementType? =
        dsl.update(ACHIEVEMENT_TYPES)
            .set(ACHIEVEMENT_TYPES.CODE, type.code)
            .set(ACHIEVEMENT_TYPES.NAME, type.name)
            .set(ACHIEVEMENT_TYPES.DESCRIPTION, type.description)
            .set(ACHIEVEMENT_TYPES.POINTS_AWARDED, type.pointsAwarded)
            .set(ACHIEVEMENT_TYPES.ICON, type.iconKey)
            .set(ACHIEVEMENT_TYPES.IS_REPEATABLE, type.isRepeatable)
            .set(ACHIEVEMENT_TYPES.IS_ACTIVE, type.isActive)
            .where(ACHIEVEMENT_TYPES.ACHIEVEMENT_TYPE_ID.eq(type.achievementTypeId))
            .returning()
            .fetchOne()
            ?.into(AchievementTypesRecord::class.java)
            ?.let { mapToEntity(it) }

    fun delete(typeId: UUID) =
        dsl.deleteFrom(ACHIEVEMENT_TYPES)
            .where(ACHIEVEMENT_TYPES.ACHIEVEMENT_TYPE_ID.eq(typeId))
            .execute()

    private fun mapToEntity(record: AchievementTypesRecord) =
        AchievementType(
            achievementTypeId = record.achievementTypeId,
            code = record.code,
            name = record.name,
            description = record.description,
            pointsAwarded = record.pointsAwarded,
            iconKey = record.icon,
            isRepeatable = record.isRepeatable,
            isActive = record.isActive,
            conditions = emptyList(),
        )
}
