package soaringLizards.blps.camunda.delegate

import org.camunda.bpm.engine.delegate.DelegateExecution
import org.camunda.bpm.engine.delegate.JavaDelegate
import org.springframework.stereotype.Component
import soaringLizards.blps.service.RewardService
import java.util.*

@Component
class RedeemRewardDelegate(
    private val rewardService: RewardService,
) : JavaDelegate {

    override fun execute(execution: DelegateExecution) {
        val userId = UUID.fromString(execution.getVariable("userId") as String)
        val rewardId = UUID.fromString(execution.getVariable("rewardId") as String)

        val result = rewardService.redeemReward(userId, rewardId)
        execution.setVariable("redeemResult", result)
        execution.setVariable("redeemedUserRewardId", result.userRewardId)
    }
}
