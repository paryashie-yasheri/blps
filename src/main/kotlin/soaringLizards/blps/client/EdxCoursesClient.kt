package soaringLizards.blps.client

import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import soaringLizards.blps.dto.edx.*

@Component
class EdxCoursesClient(
    val edxApiClientFactory: EdxApiClientFactory,
) {

    fun getRestClient(edxAuthenticatedSession: EdxAuthenticatedSession) =
        edxApiClientFactory.createForUser(edxAuthenticatedSession)

    fun listCourses(
        session: EdxAuthenticatedSession,
        page: Int? = null,
        pageSize: Int? = null,
    ) =
        getRestClient(session).get()
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


    fun getCourse(session: EdxAuthenticatedSession, courseKey: String) =
        getRestClient(session)
            .get()
            .uri("/courses/v1/courses/{courseKey}", courseKey)
            .accept(MediaType.APPLICATION_JSON)
            .retrieve()
            .body(EdxCourse::class.java)!!

    fun getCourseMetadata(
        session: EdxAuthenticatedSession,
        courseKey: String,
    ) =
        getRestClient(session)
            .get()
            .uri("/course_home/v1/course_metadata/{courseKey}", courseKey)
            .accept(MediaType.APPLICATION_JSON)
            .retrieve()
            .body(EdxCourseMetadataResponse::class.java)!!

    fun getCourseOutline(
        session: EdxAuthenticatedSession,
        courseKey: String,
    ) =
        getRestClient(session)
            .get()
            .uri("/course_home/v1/outline/{courseKey}", courseKey)
            .accept(MediaType.APPLICATION_JSON)
            .retrieve()
            .body(EdxCourseOutlineResponse::class.java)!!
}
