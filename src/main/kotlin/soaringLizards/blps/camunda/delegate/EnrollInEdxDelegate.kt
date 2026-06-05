package soaringLizards.blps.camunda.delegate

import org.camunda.bpm.engine.delegate.DelegateExecution
import org.camunda.bpm.engine.delegate.JavaDelegate
import org.springframework.stereotype.Component
import soaringLizards.blps.config.userDetails.JwtUserDetails
import soaringLizards.blps.dto.edx.EdxEnrollmentRequest
import soaringLizards.blps.service.EdxService
import java.util.*

@Component
class EnrollInEdxDelegate(
    private val edxService: EdxService,
) : JavaDelegate {

    override fun execute(execution: DelegateExecution) {
        val principal = org.springframework.security.core.context.SecurityContextHolder.getContext().authentication?.principal
        val currentUser = principal as? JwtUserDetails
        if (currentUser == null) {
            execution.setVariable("enrollmentResult", null)
            execution.setVariable("enrollSkipped", true)
            return
        }
        val courseId = execution.getVariable("courseId") as String

        val enrollment = edxService.enroll(
            currentUser,
            EdxEnrollmentRequest(
                courseId = courseId,
                courseDetails = null,
                mode = "audit",
                user = currentUser.username,
            ),
        )

        execution.setVariable("enrollmentResult", enrollment)
    }
}
