package soaringLizards.blps.dto.achievementTypes

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

// todo: дописать другие енамы в соответствии с едексом и бпмн
enum class AchievementEventType(
    @get:JsonValue
    val value: String,
) {
    LESSON_COMPLETE("lesson_complete");

    companion object {
        @JvmStatic
        @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
        fun fromValue(value: String): AchievementEventType =
            entries.firstOrNull { it.value == value }
                ?: throw IllegalArgumentException("Unsupported achievement event type: $value")
    }
}
