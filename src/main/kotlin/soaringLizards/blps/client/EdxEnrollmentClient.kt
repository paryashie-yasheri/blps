package soaringLizards.blps.client

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import soaringLizards.blps.dto.edx.EdxEnrollmentRequest
import soaringLizards.blps.dto.edx.EdxEnrollmentResponse

@Component
class EdxEnrollmentClient(
    @Qualifier("edxRestClient")
    private val edxRestClient: RestClient,
) {
    fun enrollCurrentUser(request: EdxEnrollmentRequest): EdxEnrollmentResponse? =
        edxRestClient.post()
            .uri("/enrollment/v1/enrollment")
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON)
            .body(request)
            .retrieve()
            .body(EdxEnrollmentResponse::class.java)

    fun getMyEnrollment(courseId: String): EdxEnrollmentResponse =
        edxRestClient.get()
            .uri("/enrollment/v1/enrollment/{courseId}", courseId)
            .accept(MediaType.APPLICATION_JSON)
            .retrieve()
            .body(EdxEnrollmentResponse::class.java)!!

    fun getUserEnrollment(username: String, courseId: String): EdxEnrollmentResponse =
        edxRestClient.get()
            .uri("/enrollment/v1/enrollment/{username},{courseId}", username, courseId)
            .accept(MediaType.APPLICATION_JSON)
            .retrieve()
            .body(EdxEnrollmentResponse::class.java)!!
}
