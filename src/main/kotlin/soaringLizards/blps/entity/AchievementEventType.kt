package soaringLizards.blps.entity

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

enum class AchievementEventType(
    @get:JsonValue
    val value: String,
) {
    LESSON_COMPLETE("lesson_complete"),
    SUBSECTION_COMPLETE("subsection_complete"),
    COURSE_PROGRESS_UPDATED("course_progress_updated"),
    COURSE_COMPLETE("course_complete"),
    COURSE_ENROLLED("course_enrolled");

    companion object {
        @JvmStatic
        @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
        fun fromValue(value: String): AchievementEventType =
            entries.firstOrNull { it.value == value }
                ?: throw IllegalArgumentException("Unsupported achievement event type: $value")

        fun fromValueOrNull(value: String?): AchievementEventType? =
            value?.let(::fromValue)
    }
}
