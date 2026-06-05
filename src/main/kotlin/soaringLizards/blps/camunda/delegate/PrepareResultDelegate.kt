package soaringLizards.blps.camunda.delegate

import org.camunda.bpm.engine.delegate.DelegateExecution
import org.camunda.bpm.engine.delegate.JavaDelegate
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import soaringLizards.blps.service.UserBalanceService
import java.util.*

@Component("prepareResultDelegate")
class PrepareResultDelegate(
    private val userBalanceService: UserBalanceService,
) : JavaDelegate {
    private val logger = LoggerFactory.getLogger(javaClass)

    override fun execute(execution: DelegateExecution) {
        val authToken = execution.getVariable("authToken") as? String
        if (authToken != null) {
            logger.info("auth_token_available token_len={} delegate=prepareResult", authToken.length)
        }

        val userId = UUID.fromString(execution.getVariable("userId") as String)
        val balance = userBalanceService.getCurrentUserBalance(userId)

        execution.setVariable("newBalance", balance.balance)
        execution.setVariable("operationResult", "Награда получена")
    }
}
