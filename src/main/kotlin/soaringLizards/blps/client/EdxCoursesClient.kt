package soaringLizards.blps.client

import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import soaringLizards.blps.dto.edx.*

@Component
class EdxCoursesClient(
    val edxApiClientFactory: EdxApiClientFactory,
) {
    fun listCourses(
        edxAuthenticatedSession: EdxAuthenticatedSession,
        page: Int? = null,
        pageSize: Int? = null,
    ) =
        edxApiClientFactory.createForUser(edxAuthenticatedSession).get()
            .uri { builder ->
                builder.path("/courses/v1/courses/")
                if (page != null)
                    builder.queryParam("page", page)
                if (pageSize != null)
                    builder.queryParam("page_size", pageSize)
                builder.build()
            }
            .accept(MediaType.APPLICATION_JSON)
            .retrieve()
            .body(EdxCoursesResponse::class.java)!!


    fun getCourse(edxAuthenticatedSession: EdxAuthenticatedSession, courseKey: String) =
        edxApiClientFactory
            .createForUser(edxAuthenticatedSession)
            .get()
            .uri("/courses/v1/courses/{courseKey}", courseKey)
            .accept(MediaType.APPLICATION_JSON)
            .retrieve()
            .body(EdxCourse::class.java)!!

    fun getCourseMetadata(
        edxAuthenticatedSession: EdxAuthenticatedSession,
        courseKey: String,
    ) =
        edxApiClientFactory
            .createForUser(edxAuthenticatedSession)
            .get()
            .uri("/course_home/v1/course_metadata/{courseKey}", courseKey)
            .accept(MediaType.APPLICATION_JSON)
            .retrieve()
            .body(EdxCourseMetadataResponse::class.java)!!

    fun getCourseOutline(
        edxAuthenticatedSession: EdxAuthenticatedSession,
        courseKey: String,
    ) =
        edxApiClientFactory
            .createForUser(edxAuthenticatedSession)
            .get()
            .uri("/course_home/v1/outline/{courseKey}", courseKey)
            .accept(MediaType.APPLICATION_JSON)
            .retrieve()
            .body(EdxCourseOutlineResponse::class.java)!!
}
