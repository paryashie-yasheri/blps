package soaringLizards.blps.client

import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import soaringLizards.blps.dto.edx.EdxAuthenticatedSession
import soaringLizards.blps.dto.edx.EdxEnrollmentCourseResponseDetails
import soaringLizards.blps.dto.edx.EdxEnrollmentRequest
import soaringLizards.blps.dto.edx.EdxEnrollmentResponse
import tools.jackson.databind.JsonNode
import tools.jackson.databind.ObjectMapper
import java.time.OffsetDateTime

@Component
class EdxEnrollmentClient(
    val edxApiClientFactory: EdxApiClientFactory,
    private val objectMapper: ObjectMapper,
) {
    fun getRestClient(edxAuthenticatedSession: EdxAuthenticatedSession) =
        edxApiClientFactory.createForUser(edxAuthenticatedSession)

    fun enrollCurrentUser(session: EdxAuthenticatedSession, request: EdxEnrollmentRequest): EdxEnrollmentResponse? =
        getRestClient(session)
            .post()
            .uri("/enrollment/v1/enrollment")
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON)
            .body(objectMapper.writeValueAsString(request.toOpenedxPayload()))
            .retrieve()
            .body(String::class.java)
            ?.let(::parseEnrollmentResponse)

    fun getMyEnrollment(session: EdxAuthenticatedSession, courseId: String): EdxEnrollmentResponse =
        getRestClient(session)
            .get()
            .uri("/enrollment/v1/enrollment/{courseId}", courseId)
            .accept(MediaType.APPLICATION_JSON)
            .retrieve()
            .body(String::class.java)!!
            .let(::parseEnrollmentResponse)

    fun getUserEnrollment(session: EdxAuthenticatedSession, username: String, courseId: String): EdxEnrollmentResponse =
        getRestClient(session)
            .get()
            .uri("/enrollment/v1/enrollment/{username},{courseId}", username, courseId)
            .accept(MediaType.APPLICATION_JSON)
            .retrieve()
            .body(String::class.java)!!
            .let(::parseEnrollmentResponse)

    private fun EdxEnrollmentRequest.toOpenedxPayload(): Map<String, Any> {
        val courseId = courseDetails?.courseId ?: courseId
        require(!courseId.isNullOrBlank()) {
            "courseId or courseDetails.courseId is required"
        }

        return buildMap {
            put("course_details", mapOf("course_id" to courseId))
            mode?.let { put("mode", it) }
            isActive?.let { put("is_active", it) }
            emailOptIn?.let { put("email_opt_in", it) }
            force?.let { put("force", it) }
            user?.let { put("user", it) }
            enterpriseCourseConsent?.let { put("enterprise_course_consent", it) }
            enrollmentAttributes?.let { put("enrollment_attributes", it) }
        }
    }

    private fun parseEnrollmentResponse(raw: String): EdxEnrollmentResponse {
        val json = objectMapper.readTree(raw)
        val courseDetails = json.path("course_details").takeUnless { it.isMissingNode || it.isNull }?.let {
            EdxEnrollmentCourseResponseDetails(
                courseId = it.textOrNull("course_id"),
                courseName = it.textOrNull("course_name"),
                enrollmentStart = it.offsetDateTimeOrNull("enrollment_start"),
                enrollmentEnd = it.offsetDateTimeOrNull("enrollment_end"),
                courseStart = it.offsetDateTimeOrNull("course_start"),
                courseEnd = it.offsetDateTimeOrNull("course_end"),
                inviteOnly = it.booleanOrNull("invite_only"),
                courseModes = emptyList(),
                pacingType = it.textOrNull("pacing_type"),
            )
        }

        return EdxEnrollmentResponse(
            created = json.offsetDateTimeOrNull("created"),
            mode = json.textOrNull("mode"),
            isActive = json.booleanOrNull("is_active"),
            user = json.textOrNull("user"),
            courseId = json.textOrNull("course_id") ?: courseDetails?.courseId,
            courseDetails = courseDetails,
        )
    }

    private fun JsonNode.textOrNull(fieldName: String): String? =
        path(fieldName).takeUnless { it.isMissingNode || it.isNull }?.asString()

    private fun JsonNode.booleanOrNull(fieldName: String): Boolean? =
        path(fieldName).takeUnless { it.isMissingNode || it.isNull }?.asBoolean()

    private fun JsonNode.offsetDateTimeOrNull(fieldName: String): OffsetDateTime? =
        textOrNull(fieldName)?.let(OffsetDateTime::parse)
}
