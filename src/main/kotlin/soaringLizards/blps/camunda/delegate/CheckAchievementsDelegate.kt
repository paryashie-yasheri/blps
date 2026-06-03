package soaringLizards.blps.camunda.delegate

import org.camunda.bpm.engine.delegate.DelegateExecution
import org.camunda.bpm.engine.delegate.JavaDelegate
import org.springframework.stereotype.Component
import soaringLizards.blps.entity.AchievementEventType
import soaringLizards.blps.service.AchievementContext
import soaringLizards.blps.service.AchievementService
import java.util.*

@Component
class CheckAchievementsDelegate(
    private val achievementService: AchievementService,
) : JavaDelegate {

    override fun execute(execution: DelegateExecution) {
        val userId = UUID.fromString(execution.getVariable("userId") as String)
        val eventType = AchievementEventType.valueOf(execution.getVariable("eventType") as String)
        val courseKey = execution.getVariable("courseKey") as String
        val blockId = execution.getVariable("blockId") as String?
        val completion = execution.getVariable("completion") as Double?

        val context = AchievementContext(
            eventType = eventType,
            courseKey = courseKey,
            blockId = blockId,
            completion = completion,
        )

        val awarded = achievementService.checkAndAwardAchievements(userId, context)

        val existingAwarded = execution.getVariable("awardedAchievements") as? MutableList<*> ?: mutableListOf<Any>()
        @Suppress("UNCHECKED_CAST")
        val list = existingAwarded.toMutableList()
        list.addAll(awarded)
        execution.setVariable("awardedAchievements", list)
    }
}
