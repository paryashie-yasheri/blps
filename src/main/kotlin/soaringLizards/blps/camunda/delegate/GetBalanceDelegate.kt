package soaringLizards.blps.camunda.delegate

import org.camunda.bpm.engine.delegate.DelegateExecution
import org.camunda.bpm.engine.delegate.JavaDelegate
import org.springframework.stereotype.Component
import soaringLizards.blps.service.UserBalanceService
import java.util.*

@Component
class GetBalanceDelegate(
    private val userBalanceService: UserBalanceService,
) : JavaDelegate {

    override fun execute(execution: DelegateExecution) {
        val userId = UUID.fromString(execution.getVariable("userId") as String)
        val balance = userBalanceService.getCurrentUserBalance(userId)
        execution.setVariable("currentBalance", balance.balance)
    }
}
