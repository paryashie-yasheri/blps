package soaringLizards.blps.camunda.delegate

import org.camunda.bpm.engine.delegate.DelegateExecution
import org.camunda.bpm.engine.delegate.JavaDelegate
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component("genericDelegate")
class GenericDelegate : JavaDelegate {

    private val logger = LoggerFactory.getLogger(GenericDelegate::class.java)

    override fun execute(execution: DelegateExecution) {
        logger.info("GenericDelegate: {} (activityId={})", execution.currentActivityName, execution.currentActivityId)
    }
}
