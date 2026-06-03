package soaringLizards.blps.camunda.delegate

import org.camunda.bpm.engine.delegate.DelegateExecution
import org.camunda.bpm.engine.delegate.JavaDelegate
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import soaringLizards.blps.config.userDetails.JwtUserDetails
import soaringLizards.blps.dto.edx.EdxEnrollmentRequest
import soaringLizards.blps.service.EdxService

@Component
class EnrollInEdxDelegate(
    private val edxService: EdxService,
) : JavaDelegate {

    override fun execute(execution: DelegateExecution) {
        val currentUser = SecurityContextHolder.getContext().authentication?.principal as JwtUserDetails
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
