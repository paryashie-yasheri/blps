package soaringLizards.blps

import org.springframework.stereotype.Service
import soaringLizards.blps.client.EdxCoursesClient
import soaringLizards.blps.client.EdxEnrollmentClient
import soaringLizards.blps.client.EdxProgressClient
import soaringLizards.blps.dto.edx.EdxCompletionBatchRequest
import soaringLizards.blps.dto.edx.EdxEnrollmentRequest

@Service
class EdxService(
    private val edxCoursesClient: EdxCoursesClient,
    private val edxEnrollmentClient: EdxEnrollmentClient,
    private val edxProgressClient: EdxProgressClient,
) {
    fun getCourses(page: Int? = null, pageSize: Int? = null) =
        edxCoursesClient.listCourses(page, pageSize)

    fun getCourse(courseKey: String) =
        edxCoursesClient.getCourse(courseKey)

    fun getCourseMetadata(courseKey: String) =
        edxCoursesClient.getCourseMetadata(courseKey)

    fun getCourseOutline(courseKey: String) =
        edxCoursesClient.getCourseOutline(courseKey)

    fun enroll(request: EdxEnrollmentRequest): Any? {
        require(request.courseId != null || request.courseDetails?.courseId != null) {
            "courseId or courseDetails.courseId is required"
        }

        return edxEnrollmentClient.enrollCurrentUser(request)
    }

    fun getMyEnrollment(courseId: String) =
        edxEnrollmentClient.getMyEnrollment(courseId)

    fun getUserEnrollment(username: String, courseId: String) =
        edxEnrollmentClient.getUserEnrollment(username, courseId)

    fun getProgress(courseKey: String) =
        edxProgressClient.getProgress(courseKey)

    fun getProgress(courseKey: String, studentId: String) =
        edxProgressClient.getProgress(courseKey, studentId)

    fun trackCompletion(request: EdxCompletionBatchRequest) =
        edxProgressClient.trackCompletion(request)

    fun getSubsectionCompletion(
        username: String,
        courseKey: String,
        subsectionId: String,
    ) = edxProgressClient.getSubsectionCompletion(username, courseKey, subsectionId)
}
