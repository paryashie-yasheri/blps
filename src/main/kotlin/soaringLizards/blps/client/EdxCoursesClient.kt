package soaringLizards.blps.client

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import soaringLizards.blps.edx.model.CoursesV1CoursesList200Response

@Component
class EdxCoursesClient(
    @Qualifier("edxRestClient")
    private val edxRestClient: RestClient,
) {
    fun listCourses(page: Int? = null, pageSize: Int? = null): CoursesV1CoursesList200Response {
        val response = edxRestClient.get()
            .uri { builder ->
                builder.path("/courses/v1/courses/")
                page ?: builder.queryParam("page", page)
                pageSize ?: builder.queryParam("page_size", pageSize)
                builder.build()
            }
            .accept(MediaType.APPLICATION_JSON)
            .retrieve()
            .body(CoursesV1CoursesList200Response::class.java)

        return requireNotNull(response) { "edX returned an empty response body for /courses/v1/courses/" }
    }
}
