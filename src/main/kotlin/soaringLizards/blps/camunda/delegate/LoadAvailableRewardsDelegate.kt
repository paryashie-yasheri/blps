package soaringLizards.blps.camunda.delegate

import org.camunda.bpm.engine.delegate.DelegateExecution
import org.camunda.bpm.engine.delegate.JavaDelegate
import org.camunda.bpm.engine.variable.Variables
import org.springframework.stereotype.Component
import soaringLizards.blps.service.RewardService

@Component("loadAvailableRewardsDelegate")
class LoadAvailableRewardsDelegate(
    private val rewardService: RewardService,
) : JavaDelegate {

    override fun execute(execution: DelegateExecution) {
        val rewards = rewardService.getAvailableRewards()

        val availableRewards = Variables.objectValue(rewards.map { reward ->
            mapOf(
                "name" to reward.name,
                "pointsCost" to reward.pointsCost,
                "quantity" to (reward.quantity ?: ""),
                "description" to (reward.description ?: ""),
            )
        }).serializationDataFormat("application/json").create()
        execution.setVariable("availableRewards", availableRewards)

        val rewardOptions = Variables.objectValue(rewards.map { reward ->
            mapOf(
                "name" to "${reward.name} (${reward.pointsCost} баллов)",
                "value" to reward.rewardId,
            )
        }).serializationDataFormat("application/json").create()
        execution.setVariable("rewardOptions", rewardOptions)

        val rewardListText = rewards.mapIndexed { i, r ->
            "${i + 1}. ${r.name} (${r.pointsCost} баллов, осталось: ${r.quantity ?: "?"})"
        }.joinToString("\n")
        execution.setVariable("rewardListText", rewardListText)
        execution.setVariable("selectedCode", "")

        val rewardCodes = Variables.objectValue(
            rewards.mapIndexed { i, r ->
                mapOf("code" to (i + 1).toString(), "rewardId" to r.rewardId)
            }
        ).serializationDataFormat("application/json").create()
        execution.setVariable("rewardCodes", rewardCodes)

        val rewardId = execution.getVariable("rewardId") as? String
        val selectedReward = if (rewardId != null) {
            rewards.find { it.rewardId == rewardId }
        } else null
        val rewardInfo = if (selectedReward != null) {
            "${selectedReward.name} — ${selectedReward.pointsCost} баллов"
        } else {
            "Награда не найдена (ID: $rewardId)"
        }
        execution.setVariable("rewardInfo", rewardInfo)
    }
}
