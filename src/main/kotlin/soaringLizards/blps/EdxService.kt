package soaringLizards.blps

import org.springframework.stereotype.Service
import soaringLizards.blps.edx.api.CoursesApi
import soaringLizards.blps.edx.model.Course

@Service
class EdxService(
    private val coursesApi: CoursesApi,
) {
    fun getCoursesApi(data: Course) = coursesApi.coursesV1CoursesCreate(data)
}
