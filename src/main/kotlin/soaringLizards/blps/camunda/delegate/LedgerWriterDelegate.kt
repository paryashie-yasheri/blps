package soaringLizards.blps.camunda.delegate

import org.camunda.bpm.engine.delegate.DelegateExecution
import org.camunda.bpm.engine.delegate.JavaDelegate
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import soaringLizards.blps.dto.ledger.LedgerEventDto
import soaringLizards.blps.repository.RewardRepository
import soaringLizards.blps.repository.UserBalanceRepository
import soaringLizards.blps.service.LedgerEventService
import java.time.OffsetDateTime
import java.util.*

@Component
class LedgerWriterDelegate(
    private val ledgerEventService: LedgerEventService,
    private val userBalanceRepository: UserBalanceRepository,
    private val rewardRepository: RewardRepository,
) : JavaDelegate {
    private val logger = LoggerFactory.getLogger(javaClass)

    override fun execute(execution: DelegateExecution) {
        val authToken = execution.getVariable("authToken") as? String
        if (authToken != null) {
            logger.info("auth_token_available token_len={} delegate=ledgerWriter", authToken.length)
        }

        val operationId = UUID.fromString(execution.getVariable("operationId") as String)
        val userId = UUID.fromString(execution.getVariable("userId") as String)
        val rewardId = UUID.fromString(execution.getVariable("rewardId") as String)
        val occurredAt = execution.getVariable("occurredAt") as? String
            ?: OffsetDateTime.now().toString()

        val balance = userBalanceRepository.findByUserId(userId)
            ?: throw IllegalStateException("User balance not found for userId=$userId")
        val reward = rewardRepository.findById(rewardId)
            ?: throw IllegalStateException("Reward not found for rewardId=$rewardId")

        val pointsCost = reward.pointsCost ?: 0
        val balanceBefore = balance.currentBalance!! + pointsCost
        val balanceAfter = balance.currentBalance!!

        val event = LedgerEventDto(
            operationId = operationId,
            eventType = "LEDGER_ENTRY",
            occurredAt = OffsetDateTime.parse(occurredAt),
            userId = userId,
            balanceId = balance.balanceId!!,
            amount = -pointsCost,
            balanceBefore = balanceBefore,
            balanceAfter = balanceAfter,
            rewardId = rewardId,
            description = "Ledger entry for reward: ${reward.name}",
        )
        ledgerEventService.append(event)
        execution.setVariable("ledgerWritten", true)
        logger.info("ledger_entry_written userId={} rewardId={} cost={}", userId, rewardId, pointsCost)
    }
}
