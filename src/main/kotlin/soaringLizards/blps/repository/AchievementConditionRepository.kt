package soaringLizards.blps.repository

import org.jooq.DSLContext
import org.jooq.Field
import org.jooq.Table
import org.jooq.impl.DSL
import org.jooq.impl.SQLDataType
import org.springframework.stereotype.Repository
import soaringLizards.blps.entity.AchievementCondition
import soaringLizards.blps.entity.AchievementEventType
import java.time.OffsetDateTime
import java.util.UUID

@Repository
class AchievementConditionRepository(
    private val dsl: DSLContext,
) {
    fun findByAchievementTypeIds(achievementTypeIds: Collection<UUID>): Map<UUID, List<AchievementCondition>> {
        if (achievementTypeIds.isEmpty())
            return emptyMap()

        return dsl.select(
            CONDITION_ID,
            ACHIEVEMENT_TYPE_ID,
            EVENT_TYPE,
            COURSE_PATTERN,
            BLOCK_PATTERN,
            MIN_COMPLETION,
        )
            .from(ACHIEVEMENT_CONDITIONS)
            .where(ACHIEVEMENT_TYPE_ID.`in`(achievementTypeIds))
            .orderBy(ACHIEVEMENT_TYPE_ID.asc(), CREATED_AT.asc(), CONDITION_ID.asc())
            .fetch()
            .groupBy(
                { it.get(ACHIEVEMENT_TYPE_ID)!! },
                {
                    AchievementCondition(
                        conditionId = it.get(CONDITION_ID),
                        achievementTypeId = it.get(ACHIEVEMENT_TYPE_ID),
                        eventType = AchievementEventType.fromValue(it.get(EVENT_TYPE)!!),
                        coursePattern = it.get(COURSE_PATTERN),
                        blockPattern = it.get(BLOCK_PATTERN),
                        minCompletion = it.get(MIN_COMPLETION),
                    )
                },
            )
    }

    fun replaceForAchievementType(
        achievementTypeId: UUID,
        conditions: List<AchievementCondition>,
    ) {
        deleteByAchievementTypeId(achievementTypeId)
        conditions.forEach { condition ->
            dsl.insertInto(ACHIEVEMENT_CONDITIONS)
                .set(ACHIEVEMENT_TYPE_ID, achievementTypeId)
                .set(EVENT_TYPE, condition.eventType.value)
                .set(COURSE_PATTERN, condition.coursePattern)
                .set(BLOCK_PATTERN, condition.blockPattern)
                .set(MIN_COMPLETION, condition.minCompletion)
                .execute()
        }
    }

    fun deleteByAchievementTypeId(achievementTypeId: UUID) =
        dsl.deleteFrom(ACHIEVEMENT_CONDITIONS)
            .where(ACHIEVEMENT_TYPE_ID.eq(achievementTypeId))
            .execute()

    companion object {
        private val ACHIEVEMENT_CONDITIONS: Table<*> = DSL.table(DSL.name("achievement_conditions"))
        private val CONDITION_ID: Field<UUID> = DSL.field(DSL.name("condition_id"), SQLDataType.UUID.nullable(false))
        private val ACHIEVEMENT_TYPE_ID: Field<UUID> = DSL.field(
            DSL.name("achievement_type_id"),
            SQLDataType.UUID.nullable(false),
        )
        private val EVENT_TYPE: Field<String> = DSL.field(
            DSL.name("event_type"),
            SQLDataType.VARCHAR(100).nullable(false),
        )
        private val COURSE_PATTERN: Field<String> = DSL.field(
            DSL.name("course_pattern"),
            SQLDataType.VARCHAR(255),
        )
        private val BLOCK_PATTERN: Field<String> = DSL.field(
            DSL.name("block_pattern"),
            SQLDataType.VARCHAR(255),
        )
        private val MIN_COMPLETION: Field<Double> = DSL.field(
            DSL.name("min_completion"),
            SQLDataType.DOUBLE,
        )
        private val CREATED_AT: Field<OffsetDateTime> = DSL.field(
            DSL.name("created_at"),
            SQLDataType.TIMESTAMPWITHTIMEZONE,
        )
    }
}
