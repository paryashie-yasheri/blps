package soaringLizards.blps.client

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import soaringLizards.blps.dto.edx.EdxCompletionBatchRequest
import soaringLizards.blps.dto.edx.EdxCompletionBatchResponse
import soaringLizards.blps.dto.edx.EdxCourseProgressResponse
import soaringLizards.blps.dto.edx.EdxSubsectionCompletionResponse

@Component
class EdxProgressClient(
    @Qualifier("edxRestClient")
    private val edxRestClient: RestClient,
) {
    fun getProgress(courseKey: String): EdxCourseProgressResponse =
        edxRestClient.get()
            .uri("/course_home/v1/progress/{courseKey}", courseKey)
            .accept(MediaType.APPLICATION_JSON)
            .retrieve()
            .body(EdxCourseProgressResponse::class.java)!!

    fun getProgress(courseKey: String, studentId: String): EdxCourseProgressResponse =
        edxRestClient.get()
            .uri("/course_home/v1/progress/{courseKey}/{studentId}", courseKey, studentId)
            .accept(MediaType.APPLICATION_JSON)
            .retrieve()
            .body(EdxCourseProgressResponse::class.java)!!

    fun trackCompletion(request: EdxCompletionBatchRequest): EdxCompletionBatchResponse? =
        edxRestClient.post()
            .uri("/completion/v1/completion-batch")
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON)
            .body(request)
            .retrieve()
            .body(EdxCompletionBatchResponse::class.java)

    fun getSubsectionCompletion(
        username: String,
        courseKey: String,
        subsectionId: String,
    ): EdxSubsectionCompletionResponse =
        edxRestClient.get()
            .uri(
                "/completion/v1/subsection-completion/{username}/{courseKey}/{subsectionId}",
                username,
                courseKey,
                subsectionId,
            )
            .accept(MediaType.APPLICATION_JSON)
            .retrieve()
            .body(EdxSubsectionCompletionResponse::class.java)!!
}
