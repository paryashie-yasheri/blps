package soaringLizards.blps.camunda.delegate

import org.camunda.bpm.engine.delegate.DelegateExecution
import org.camunda.bpm.engine.delegate.JavaDelegate
import org.springframework.stereotype.Component
import soaringLizards.blps.config.userDetails.JwtUserDetails
import soaringLizards.blps.dto.edx.EdxCompletionBatchRequest
import soaringLizards.blps.service.EdxService
import org.springframework.security.core.context.SecurityContextHolder

@Component
class TrackLessonCompletionDelegate(
    private val edxService: EdxService,
) : JavaDelegate {

    override fun execute(execution: DelegateExecution) {
        val currentUser = SecurityContextHolder.getContext().authentication?.principal as JwtUserDetails
        val courseKey = execution.getVariable("courseKey") as String
        val blockId = execution.getVariable("blockId") as String

        val result = runCatching {
            edxService.trackCompletion(
                currentUser,
                EdxCompletionBatchRequest(
                    username = currentUser.username,
                    courseKey = courseKey,
                    blocks = mapOf(blockId to 1.0),
                ),
            )
        }

        execution.setVariable("trackCompletionSuccess", result.isSuccess)
        execution.setVariable("trackCompletionResult", result.getOrNull())
    }
}
