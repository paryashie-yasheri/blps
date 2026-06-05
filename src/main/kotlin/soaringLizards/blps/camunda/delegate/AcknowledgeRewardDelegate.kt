package soaringLizards.blps.camunda.delegate

import org.camunda.bpm.engine.delegate.DelegateExecution
import org.camunda.bpm.engine.delegate.JavaDelegate
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import soaringLizards.blps.repository.RewardRepository
import java.util.*

@Component
class AcknowledgeRewardDelegate(
    private val rewardRepository: RewardRepository,
) : JavaDelegate {
    private val logger = LoggerFactory.getLogger(javaClass)

    override fun execute(execution: DelegateExecution) {
        val authToken = execution.getVariable("authToken") as? String
        if (authToken != null) {
            logger.info("auth_token_available token_len={} delegate=acknowledgeReward", authToken.length)
        }

        val redeemResult = execution.getVariable("redeemResult")
        val userId = UUID.fromString(execution.getVariable("userId") as String)
        val rewardId = UUID.fromString(execution.getVariable("rewardId") as String)
        val reward = rewardRepository.findById(rewardId)

        val rewardName = reward?.name ?: "Unknown"
        val assigned = redeemResult != null

        execution.setVariable("rewardAssigned", assigned)
        execution.setVariable("rewardName", rewardName)
        if (assigned) {
            logger.info("reward_assigned userId={} reward={} userRewardId={}",
                userId, rewardName, execution.getVariable("redeemedUserRewardId"))
        } else {
            logger.warn("reward_not_assigned userId={} reward={} (redeemResult missing)", userId, rewardName)
        }
    }
}
