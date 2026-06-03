package soaringLizards.blps.camunda.delegate

import org.camunda.bpm.engine.delegate.DelegateExecution
import org.camunda.bpm.engine.delegate.JavaDelegate
import org.springframework.stereotype.Component
import soaringLizards.blps.service.RewardRedemptionEventPublisher
import soaringLizards.blps.dto.events.RewardRedemptionEvent
import java.util.*

@Component
class PublishRedemptionEventDelegate(
    private val publisher: RewardRedemptionEventPublisher,
) : JavaDelegate {

    override fun execute(execution: DelegateExecution) {
        val operationId = UUID.fromString(execution.getVariable("operationId") as String)
        val userId = UUID.fromString(execution.getVariable("userId") as String)
        val rewardId = UUID.fromString(execution.getVariable("rewardId") as String)
        val userRewardIdStr = execution.getVariable("redeemedUserRewardId") as? String
        val userRewardId = if (userRewardIdStr != null) UUID.fromString(userRewardIdStr) else UUID.randomUUID()
        val occurredAt = execution.getVariable("occurredAt") as? String
            ?: java.time.OffsetDateTime.now().toString()

        publisher.publishAfterCommit(
            RewardRedemptionEvent(
                operationId = operationId,
                userId = userId,
                rewardId = rewardId,
                userRewardId = userRewardId,
                occurredAt = occurredAt,
            ),
        )
    }
}
