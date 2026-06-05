package soaringLizards.blps.service

import org.apache.pulsar.client.api.Producer
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.ObjectProvider
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.core.task.TaskExecutor
import org.springframework.stereotype.Service
import org.springframework.transaction.support.TransactionSynchronization
import org.springframework.transaction.support.TransactionSynchronizationManager
import soaringLizards.blps.config.properties.PulsarProperties
import soaringLizards.blps.dto.events.RewardRedemptionEvent
import com.fasterxml.jackson.databind.ObjectMapper
import java.util.concurrent.RejectedExecutionException

@Service
class RewardRedemptionEventPublisher(
    private val producerProvider: ObjectProvider<Producer<ByteArray>>,
    private val objectMapper: ObjectMapper,
    private val properties: PulsarProperties,
    @Qualifier("rewardRedemptionEventExecutor")
    private val taskExecutor: TaskExecutor,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    fun publishAfterCommit(event: RewardRedemptionEvent, authToken: String? = null) {
        if (!properties.enabled) {
            logger.debug("reward_redemption_event_skipped reason=pulsar_disabled operationId={}", event.operationId)
            return
        }

        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(
                object : TransactionSynchronization {
                    override fun afterCommit() {
                        publishInBackground(event, authToken)
                    }
                },
            )
        } else {
            publishInBackground(event, authToken)
        }
    }

    private fun publishInBackground(event: RewardRedemptionEvent, authToken: String? = null) {
        try {
            taskExecutor.execute {
                publish(event, authToken)
            }
        } catch (ex: RejectedExecutionException) {
            logger.warn(
                "reward_redemption_event_rejected operationId={} userId={} rewardId={}",
                event.operationId,
                event.userId,
                event.rewardId,
                ex,
            )
        }
    }

    private fun publish(event: RewardRedemptionEvent, authToken: String? = null) {
        try {
            val producer = producerProvider.getIfAvailable()
            if (producer == null) {
                logger.debug("reward_redemption_event_skipped reason=producer_unavailable operationId={}", event.operationId)
                return
            }

            val props = mutableMapOf(
                "eventType" to "REWARD_REDEEMED",
                "operationId" to event.operationId.toString(),
            )
            if (authToken != null) {
                props["authToken"] = authToken
            }

            producer.newMessage()
                .key(event.userId.toString())
                .properties(props)
                .value(objectMapper.writeValueAsBytes(event))
                .sendAsync()
                .whenComplete { messageId, throwable ->
                    if (throwable != null) {
                        logger.warn(
                            "reward_redemption_event_publish_failed operationId={} userId={} rewardId={}",
                            event.operationId,
                            event.userId,
                            event.rewardId,
                            throwable,
                        )
                    } else {
                        logger.debug(
                            "reward_redemption_event_published operationId={} messageId={}",
                            event.operationId,
                            messageId,
                        )
                    }
                }
        } catch (ex: Exception) {
            logger.warn(
                "reward_redemption_event_publish_failed operationId={} userId={} rewardId={}",
                event.operationId,
                event.userId,
                event.rewardId,
                ex,
            )
        }
    }
}
