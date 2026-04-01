package soaringLizards.blps.controller

import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import soaringLizards.blps.EdxService
import soaringLizards.blps.dto.edx.EdxCompletionBatchRequest
import soaringLizards.blps.dto.edx.EdxEnrollmentRequest

@RestController
@RequestMapping("/api/edx")
@Tag(name = "Контроллер прослойка для общения с Tutor")
class EdxController(
    private val edxService: EdxService,
) {
    @GetMapping("/courses")
    fun getAllCourses(
        @RequestParam(required = false) page: Int?,
        @RequestParam(required = false) pageSize: Int?,
    ) = ResponseEntity.ok(edxService.getCourses(page, pageSize))

    @GetMapping("/courses/{courseKey}")
    fun getCourse(
        @PathVariable courseKey: String,
    ) = ResponseEntity.ok(edxService.getCourse(courseKey))

    @GetMapping("/courses/{courseKey}/metadata")
    fun getCourseMetadata(
        @PathVariable courseKey: String,
    ) = ResponseEntity.ok(edxService.getCourseMetadata(courseKey))

    @GetMapping("/courses/{courseKey}/outline")
    fun getCourseOutline(
        @PathVariable courseKey: String,
    ) = ResponseEntity.ok(edxService.getCourseOutline(courseKey))

    @PostMapping("/enrollment")
    fun enroll(
        @RequestBody request: EdxEnrollmentRequest,
    ) = ResponseEntity.ok(edxService.enroll(request))

    @GetMapping("/courses/{courseId}/enrollment")
    fun getMyEnrollment(
        @PathVariable courseId: String,
    ) = ResponseEntity.ok(edxService.getMyEnrollment(courseId))

    @GetMapping("/users/{username}/courses/{courseId}/enrollment")
    fun getUserEnrollment(
        @PathVariable username: String,
        @PathVariable courseId: String,
    ) = ResponseEntity.ok(edxService.getUserEnrollment(username, courseId))

    @GetMapping("/courses/{courseKey}/progress")
    fun getProgress(
        @PathVariable courseKey: String,
    ) = ResponseEntity.ok(edxService.getProgress(courseKey))

    @GetMapping("/courses/{courseKey}/progress/{studentId}")
    fun getProgressForStudent(
        @PathVariable courseKey: String,
        @PathVariable studentId: String,
    ) = ResponseEntity.ok(edxService.getProgress(courseKey, studentId))

    @PostMapping("/completion-batch")
    fun trackCompletion(
        @RequestBody request: EdxCompletionBatchRequest,
    ) = ResponseEntity.ok(edxService.trackCompletion(request))

    @GetMapping("/users/{username}/courses/{courseKey}/subsections/{subsectionId}/completion")
    fun getSubsectionCompletion(
        @PathVariable username: String,
        @PathVariable courseKey: String,
        @PathVariable subsectionId: String,
    ) = ResponseEntity.ok(edxService.getSubsectionCompletion(username, courseKey, subsectionId))
}
