package soaringLizards.blps.client

import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import soaringLizards.blps.dto.edx.*

@Component
class EdxProgressClient(
    val edxApiClientFactory: EdxApiClientFactory,
) {
    fun getRestClient(edxAuthenticatedSession: EdxAuthenticatedSession) =
        edxApiClientFactory.createForUser(edxAuthenticatedSession)

    fun getProgress(session: EdxAuthenticatedSession, courseKey: String): EdxCourseProgressResponse =
        getRestClient(session).get()
            .uri("/course_home/v1/progress/{courseKey}", courseKey)
            .accept(MediaType.APPLICATION_JSON)
            .retrieve()
            .body(EdxCourseProgressResponse::class.java)!!

    fun getProgress(session: EdxAuthenticatedSession, courseKey: String, studentId: String): EdxCourseProgressResponse =
        getRestClient(session).get()
            .uri("/course_home/v1/progress/{courseKey}/{studentId}", courseKey, studentId)
            .accept(MediaType.APPLICATION_JSON)
            .retrieve()
            .body(EdxCourseProgressResponse::class.java)!!

    fun trackCompletion(
        session: EdxAuthenticatedSession,
        request: EdxCompletionBatchRequest,
    ): EdxCompletionBatchResponse? =
        getRestClient(session).post()
            .uri("/completion/v1/completion-batch")
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON)
            .body(request)
            .retrieve()
            .body(EdxCompletionBatchResponse::class.java)

    fun getSubsectionCompletion(
        session: EdxAuthenticatedSession,
        username: String,
        courseKey: String,
        subsectionId: String,
    ): EdxSubsectionCompletionResponse =
        getRestClient(session).get()
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
