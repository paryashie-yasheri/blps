package soaringLizards.blps.camunda.delegate

import org.camunda.bpm.engine.delegate.DelegateExecution
import org.camunda.bpm.engine.delegate.JavaDelegate
import org.springframework.stereotype.Component
import soaringLizards.blps.config.userDetails.JwtUserDetails
import soaringLizards.blps.dto.edx.EdxCompletionBatchRequest
import soaringLizards.blps.service.EdxService

@Component
class TrackLessonCompletionDelegate(
    private val edxService: EdxService,
) : JavaDelegate {

    override fun execute(execution: DelegateExecution) {
        val courseKey = execution.getVariable("courseKey") as String
        val blockId = execution.getVariable("blockId") as String
        val userId = execution.getVariable("userId") as String

        val result = runCatching {
            val principal = org.springframework.security.core.context.SecurityContextHolder.getContext().authentication?.principal
            val currentUser = when (principal) {
                is JwtUserDetails -> principal
                else -> null
            }
            if (currentUser != null) {
                edxService.trackCompletion(
                    currentUser,
                    EdxCompletionBatchRequest(
                        username = currentUser.username,
                        courseKey = courseKey,
                        blocks = mapOf(blockId to 1.0),
                    ),
                )
            } else null
        }

        execution.setVariable("trackCompletionSuccess", result.isSuccess)
        execution.setVariable("trackCompletionResult", result.getOrNull())
    }
}
