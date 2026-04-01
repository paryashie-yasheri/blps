package soaringLizards.blps.client

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import soaringLizards.blps.dto.edx.EdxCourse
import soaringLizards.blps.dto.edx.EdxCourseMetadataResponse
import soaringLizards.blps.dto.edx.EdxCourseOutlineResponse
import soaringLizards.blps.dto.edx.EdxCoursesResponse

@Component
class EdxCoursesClient(
    @Qualifier("edxRestClient")
    private val edxRestClient: RestClient,
) {
    fun listCourses(page: Int? = null, pageSize: Int? = null): EdxCoursesResponse {
        val response = edxRestClient.get()
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

        return response.body(EdxCoursesResponse::class.java)!!
    }

    fun getCourse(courseKey: String): EdxCourse =
        edxRestClient.get()
            .uri("/courses/v1/courses/{courseKey}", courseKey)
            .accept(MediaType.APPLICATION_JSON)
            .retrieve()
            .body(EdxCourse::class.java)!!

    fun getCourseMetadata(courseKey: String): EdxCourseMetadataResponse =
        edxRestClient.get()
            .uri("/course_home/v1/course_metadata/{courseKey}", courseKey)
            .accept(MediaType.APPLICATION_JSON)
            .retrieve()
            .body(EdxCourseMetadataResponse::class.java)!!

    fun getCourseOutline(courseKey: String): EdxCourseOutlineResponse =
        edxRestClient.get()
            .uri("/course_home/v1/outline/{courseKey}", courseKey)
            .accept(MediaType.APPLICATION_JSON)
            .retrieve()
            .body(EdxCourseOutlineResponse::class.java)!!
}
