package soaringLizards.blps.camunda.delegate

import org.camunda.bpm.engine.delegate.DelegateExecution
import org.camunda.bpm.engine.delegate.JavaDelegate
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import soaringLizards.blps.repository.RewardRepository
import java.util.*

@Component
class ShowResultDelegate(
    private val rewardRepository: RewardRepository,
) : JavaDelegate {
    private val logger = LoggerFactory.getLogger(javaClass)

    override fun execute(execution: DelegateExecution) {
        val authToken = execution.getVariable("authToken") as? String
        if (authToken != null) {
            logger.info("auth_token_available token_len={} delegate=showResult", authToken.length)
        }

        val userId = UUID.fromString(execution.getVariable("userId") as String)
        val rewardId = UUID.fromString(execution.getVariable("rewardId") as String)
        val newBalance = execution.getVariable("newBalance") as? Int ?: 0
        val receiptText = execution.getVariable("receiptText") as? String ?: ""

        val reward = rewardRepository.findById(rewardId)
        val rewardName = reward?.name ?: "Unknown"

        val result = mapOf(
            "status" to "SUCCESS",
            "rewardName" to rewardName,
            "newBalance" to newBalance.toString(),
            "receipt" to receiptText,
            "message" to "Награда '$rewardName' успешно получена! Текущий баланс: $newBalance баллов",
        )

        execution.setVariable("operationResult", result.toString())
        execution.setVariable("resultMessage", "Награда '$rewardName' успешно получена! Текущий баланс: $newBalance баллов")
        logger.info("result_presented userId={} reward={} balance={}", userId, rewardName, newBalance)
    }
}
