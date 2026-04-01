package soaringLizards.blps

import org.springframework.stereotype.Service
import soaringLizards.blps.client.EdxCoursesClient

@Service
class EdxService(
    private val edxCoursesClient: EdxCoursesClient,
) {
    fun getCourses() = edxCoursesClient.listCourses()
}
