package soaringLizards.blps.service

import org.springframework.stereotype.Service
import soaringLizards.blps.client.EdxCoursesClient
import soaringLizards.blps.client.EdxEnrollmentClient
import soaringLizards.blps.client.EdxProgressClient
import soaringLizards.blps.config.userDetails.JwtUserDetails
import soaringLizards.blps.dto.edx.EdxCompletionBatchRequest
import soaringLizards.blps.dto.edx.EdxEnrollmentRequest
import soaringLizards.blps.dto.edx.EdxEnrollmentResponse

@Service
class EdxService(
    private val edxCoursesClient: EdxCoursesClient,
    private val edxEnrollmentClient: EdxEnrollmentClient,
    private val edxProgressClient: EdxProgressClient,
) {
    fun getCourses(userDetails: JwtUserDetails, page: Int? = null, pageSize: Int? = null) =
        edxCoursesClient.listCourses(userDetails.edxSession!!, page, pageSize)


    fun getCourse(userDetails: JwtUserDetails, courseKey: String) =
        edxCoursesClient.getCourse(userDetails.edxSession!!, courseKey)


    fun getCourseMetadata(userDetails: JwtUserDetails, courseKey: String) =
        edxCoursesClient.getCourseMetadata(userDetails.edxSession!!, courseKey)


    fun getCourseOutline(userDetails: JwtUserDetails, courseKey: String) =
        edxCoursesClient.getCourseOutline(userDetails.edxSession!!, courseKey)


    fun enroll(userDetails: JwtUserDetails, request: EdxEnrollmentRequest): EdxEnrollmentResponse? {
        require(request.courseId != null || request.courseDetails?.courseId != null) {
            "courseId or courseDetails.courseId is required"
        }

        return edxEnrollmentClient.enrollCurrentUser(userDetails.edxSession!!, request)
    }

    fun getMyEnrollment(userDetails: JwtUserDetails, courseId: String) =
        edxEnrollmentClient.getMyEnrollment(userDetails.edxSession!!, courseId)


    fun getUserEnrollment(userDetails: JwtUserDetails, courseId: String): EdxEnrollmentResponse {
        return edxEnrollmentClient.getUserEnrollment(userDetails.edxSession!!, userDetails.username, courseId)
    }

    fun getProgress(userDetails: JwtUserDetails, courseKey: String) =
        edxProgressClient.getProgress(userDetails.edxSession!!, courseKey)


    fun getProgress(userDetails: JwtUserDetails, courseKey: String, studentId: String) =
        edxProgressClient.getProgress(userDetails.edxSession!!, courseKey, studentId)


    fun trackCompletion(userDetails: JwtUserDetails, request: EdxCompletionBatchRequest) =
        edxProgressClient.trackCompletion(userDetails.edxSession!!, request)


    fun getSubsectionCompletion(
        userDetails: JwtUserDetails,
        courseKey: String,
        subsectionId: String,
    ) =
        edxProgressClient.getSubsectionCompletion(
            userDetails.edxSession!!,
            userDetails.username,
            courseKey,
            subsectionId,
        )
}
