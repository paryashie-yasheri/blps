package soaringLizards.blps.camunda.delegate

import org.camunda.bpm.engine.delegate.DelegateExecution
import org.camunda.bpm.engine.delegate.JavaDelegate
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import soaringLizards.blps.service.KeycloakService
import java.util.*

@Component
class AcquireTokenDelegate(
    private val keycloakService: KeycloakService,
) : JavaDelegate {
    private val logger = LoggerFactory.getLogger(javaClass)

    override fun execute(execution: DelegateExecution) {
        val userId = UUID.fromString(execution.getVariable("userId") as String)
        val adminToken = keycloakService.getAdminAccessToken()
        execution.setVariable("authToken", adminToken)
        execution.setVariable("tokenType", "Bearer")
        execution.setVariable("tokenAcquiredAt", java.time.OffsetDateTime.now().toString())
        logger.info("token_acquired userId={} token_len={}", userId, adminToken.length)
    }
}
