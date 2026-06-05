package soaringLizards.blps.camunda.delegate

import org.camunda.bpm.engine.delegate.DelegateExecution
import org.camunda.bpm.engine.delegate.JavaDelegate
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import soaringLizards.blps.dto.events.RewardRedemptionEvent
import soaringLizards.blps.service.RewardRedemptionEventPublisher
import java.util.*

@Component
class SafePublishRedemptionEventDelegate(
    private val publisher: RewardRedemptionEventPublisher,
) : JavaDelegate {
    private val logger = LoggerFactory.getLogger(javaClass)

    override fun execute(execution: DelegateExecution) {
        val authToken = execution.getVariable("authToken") as? String
        if (authToken != null) {
            logger.info("auth_token_available token_len={} delegate=safePublish", authToken.length)
        }

        try {
            val operationId = UUID.fromString(execution.getVariable("operationId") as String)
            val userId = UUID.fromString(execution.getVariable("userId") as String)
            val rewardId = UUID.fromString(execution.getVariable("rewardId") as String)
            val userRewardIdStr = execution.getVariable("redeemedUserRewardId") as? String
            val userRewardId = if (userRewardIdStr != null) UUID.fromString(userRewardIdStr) else UUID.randomUUID()
            val occurredAt = execution.getVariable("occurredAt") as? String
                ?: java.time.OffsetDateTime.now().toString()

            publisher.publishAfterCommit(
                event = RewardRedemptionEvent(
                    operationId = operationId,
                    userId = userId,
                    rewardId = rewardId,
                    userRewardId = userRewardId,
                    occurredAt = occurredAt,
                ),
                authToken = authToken,
            )
            execution.setVariable("eventPublished", true)
            logger.info("event_published_to_pulsar userId={} rewardId={}", userId, rewardId)
        } catch (e: Exception) {
            logger.warn("pulsar_not_available skip_publish userId={} rewardId={} error={}",
                execution.getVariable("userId"), execution.getVariable("rewardId"), e.message)
            execution.setVariable("eventPublished", false)
        }
    }
}
