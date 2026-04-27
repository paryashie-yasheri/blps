package soaringLizards.blps.service

import org.springframework.security.access.AccessDeniedException
import org.springframework.stereotype.Service
import soaringLizards.blps.client.EdxCoursesClient
import soaringLizards.blps.client.EdxEnrollmentClient
import soaringLizards.blps.client.EdxProgressClient
import soaringLizards.blps.config.userDetails.JwtUserDetails
import soaringLizards.blps.dto.edx.*

@Service
class EdxService(
    private val edxCoursesClient: EdxCoursesClient,
    private val edxEnrollmentClient: EdxEnrollmentClient,
    private val edxProgressClient: EdxProgressClient,
) {
    fun getCourses(userDetails: JwtUserDetails?, page: Int? = null, pageSize: Int? = null): EdxCoursesResponse {
        val session = userDetails?.edxSession ?: throw AccessDeniedException("Not logged in")
        return edxCoursesClient.listCourses(session, page, pageSize)
    }

    fun getCourse(userDetails: JwtUserDetails?, courseKey: String): EdxCourse {
        val session = userDetails?.edxSession ?: throw AccessDeniedException("Not logged in")
        return edxCoursesClient.getCourse(session, courseKey)
    }

    fun getCourseMetadata(userDetails: JwtUserDetails?, courseKey: String): EdxCourseMetadataResponse {
        val session = userDetails?.edxSession ?: throw AccessDeniedException("Not logged in")
        return edxCoursesClient.getCourseMetadata(session, courseKey)
    }

    fun getCourseOutline(userDetails: JwtUserDetails?, courseKey: String): EdxCourseOutlineResponse {
        val session = userDetails?.edxSession ?: throw AccessDeniedException("Not logged in")
        return edxCoursesClient.getCourseOutline(session, courseKey)
    }

    fun enroll(userDetails: JwtUserDetails?, request: EdxEnrollmentRequest): Any? {
        val session = userDetails?.edxSession ?: throw AccessDeniedException("Not logged in")

        require(request.courseId != null || request.courseDetails?.courseId != null) {
            "courseId or courseDetails.courseId is required"
        }

        return edxEnrollmentClient.enrollCurrentUser(session, request)
    }

    fun getMyEnrollment(userDetails: JwtUserDetails?, courseId: String): EdxEnrollmentResponse {
        val session = userDetails?.edxSession ?: throw AccessDeniedException("Not logged in")
        return edxEnrollmentClient.getMyEnrollment(session, courseId)
    }

    fun getUserEnrollment(userDetails: JwtUserDetails?, courseId: String): EdxEnrollmentResponse {
        val details = userDetails ?: throw AccessDeniedException("Not logged in")
        val session = userDetails.edxSession ?: throw AccessDeniedException("Not logged in")
        return edxEnrollmentClient.getUserEnrollment(session, details.username, courseId)
    }

    fun getProgress(userDetails: JwtUserDetails?, courseKey: String): EdxCourseProgressResponse {
        val session = userDetails?.edxSession ?: throw AccessDeniedException("Not logged in")
        return edxProgressClient.getProgress(session, courseKey)
    }

    fun getProgress(userDetails: JwtUserDetails?, courseKey: String, studentId: String): EdxCourseProgressResponse {
        val session = userDetails?.edxSession ?: throw AccessDeniedException("Not logged in")
        return edxProgressClient.getProgress(session, courseKey, studentId)
    }

    fun trackCompletion(userDetails: JwtUserDetails?, request: EdxCompletionBatchRequest): EdxCompletionBatchResponse? {
        val session = userDetails?.edxSession ?: throw AccessDeniedException("Not logged in")
        return edxProgressClient.trackCompletion(session, request)
    }

    fun getSubsectionCompletion(
        userDetails: JwtUserDetails?,
        courseKey: String,
        subsectionId: String,
    ) {
        val details = userDetails ?: throw AccessDeniedException("Not logged in")
        val session = userDetails.edxSession ?: throw AccessDeniedException("Not logged in")
        edxProgressClient.getSubsectionCompletion(session, details.username, courseKey, subsectionId)
    }
}
