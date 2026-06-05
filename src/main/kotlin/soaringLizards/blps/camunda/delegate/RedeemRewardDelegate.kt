package soaringLizards.blps.camunda.delegate

import org.camunda.bpm.engine.delegate.DelegateExecution
import org.camunda.bpm.engine.delegate.JavaDelegate
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import soaringLizards.blps.service.RewardService
import java.util.*

@Component
class RedeemRewardDelegate(
    private val rewardService: RewardService,
) : JavaDelegate {
    private val logger = LoggerFactory.getLogger(javaClass)

    override fun execute(execution: DelegateExecution) {
        val authToken = execution.getVariable("authToken") as? String
        if (authToken != null) {
            logger.info("auth_token_available token_len={} delegate=redeemReward", authToken.length)
        }

        val existingResult = execution.getVariable("redeemResult")
        if (existingResult != null) {
            return
        }

        val selectedCode = execution.getVariable("selectedCode") as? String
        if (!selectedCode.isNullOrBlank()) {
            val rewardCodes = execution.getVariable("rewardCodes") as? List<*>
            val match = rewardCodes?.find { item ->
                (item as? Map<*, *>)?.get("code") == selectedCode
            } as? Map<*, *>
            val mappedRewardId = match?.get("rewardId") as? String
            if (mappedRewardId != null) {
                execution.setVariable("rewardId", mappedRewardId)
            }
        }

        val userId = UUID.fromString(execution.getVariable("userId") as String)
        val rewardId = UUID.fromString(execution.getVariable("rewardId") as String)

        val result = rewardService.redeemReward(userId, rewardId)
        execution.setVariable("redeemResult", result)
        execution.setVariable("redeemedUserRewardId", result.userRewardId)
    }
}
