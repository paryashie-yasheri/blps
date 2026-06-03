package soaringLizards.blps.camunda.delegate

import org.camunda.bpm.engine.delegate.DelegateExecution
import org.camunda.bpm.engine.delegate.JavaDelegate
import org.slf4j.LoggerFactory
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import soaringLizards.blps.config.userDetails.JwtUserDetails
import soaringLizards.blps.service.EdxService

@Component
class FetchSubsectionProgressDelegate(
    private val edxService: EdxService,
) : JavaDelegate {

    private val logger = LoggerFactory.getLogger(javaClass)

    override fun execute(execution: DelegateExecution) {
        val currentUser = SecurityContextHolder.getContext().authentication?.principal as JwtUserDetails
        val courseKey = execution.getVariable("courseKey") as String
        val blockId = execution.getVariable("blockId") as String

        val subsectionCompletion = runCatching {
            edxService.getSubsectionCompletion(currentUser, courseKey, blockId)
        }.onFailure { error ->
            logger.warn("Failed to fetch subsection completion: ${error.message}")
        }.getOrNull()

        execution.setVariable("subsectionCompletion",
            subsectionCompletion?.completion?.toString() ?: "0.0")

        val courseProgress = runCatching {
            edxService.getProgress(currentUser, courseKey)
        }.onFailure { error ->
            logger.warn("Failed to fetch course progress: ${error.message}")
        }.getOrNull()

        execution.setVariable("courseProgress", courseProgress)
    }
}
