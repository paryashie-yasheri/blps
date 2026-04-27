package soaringLizards.blps.client

import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import soaringLizards.blps.dto.edx.EdxAuthenticatedSession
import soaringLizards.blps.dto.edx.EdxEnrollmentRequest
import soaringLizards.blps.dto.edx.EdxEnrollmentResponse

@Component
class EdxEnrollmentClient(
    val edxApiClientFactory: EdxApiClientFactory,
) {
    fun enrollCurrentUser(session: EdxAuthenticatedSession, request: EdxEnrollmentRequest): EdxEnrollmentResponse? =
        edxApiClientFactory
            .createForUser(session)
            .post()
            .uri("/enrollment/v1/enrollment")
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON)
            .body(request)
            .retrieve()
            .body(EdxEnrollmentResponse::class.java)

    fun getMyEnrollment(session: EdxAuthenticatedSession, courseId: String): EdxEnrollmentResponse =
        edxApiClientFactory
            .createForUser(session)
            .get()
            .uri("/enrollment/v1/enrollment/{courseId}", courseId)
            .accept(MediaType.APPLICATION_JSON)
            .retrieve()
            .body(EdxEnrollmentResponse::class.java)!!

    fun getUserEnrollment(session: EdxAuthenticatedSession, username: String, courseId: String): EdxEnrollmentResponse =
        edxApiClientFactory
            .createForUser(session)
            .get()
            .uri("/enrollment/v1/enrollment/{username},{courseId}", username, courseId)
            .accept(MediaType.APPLICATION_JSON)
            .retrieve()
            .body(EdxEnrollmentResponse::class.java)!!
}
