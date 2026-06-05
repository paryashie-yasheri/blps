package soaringLizards.blps.camunda.delegate

import org.camunda.bpm.engine.delegate.DelegateExecution
import org.camunda.bpm.engine.delegate.JavaDelegate
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import soaringLizards.blps.repository.RewardRepository
import java.time.OffsetDateTime
import java.util.*

@Component
class GenerateReceiptDelegate(
    private val rewardRepository: RewardRepository,
) : JavaDelegate {
    private val logger = LoggerFactory.getLogger(javaClass)

    override fun execute(execution: DelegateExecution) {
        val authToken = execution.getVariable("authToken") as? String
        if (authToken != null) {
            logger.info("auth_token_available token_len={} delegate=generateReceipt", authToken.length)
        }

        val userId = UUID.fromString(execution.getVariable("userId") as String)
        val rewardId = UUID.fromString(execution.getVariable("rewardId") as String)
        val newBalance = execution.getVariable("newBalance") as? Int ?: 0
        val reward = rewardRepository.findById(rewardId)
            ?: throw IllegalStateException("Reward not found")

        val pointsCost = reward.pointsCost ?: 0
        val receipt = """
            |Чек покупки
            |Награда: ${reward.name}
            |Списано: $pointsCost баллов
            |Остаток: $newBalance баллов
            |Дата: ${OffsetDateTime.now()}
        """.trimMargin()

        execution.setVariable("receiptText", receipt)
        execution.setVariable("receiptGeneratedAt", OffsetDateTime.now().toString())
        logger.info("receipt_generated userId={} reward={} cost={}", userId, reward.name, pointsCost)
    }
}
