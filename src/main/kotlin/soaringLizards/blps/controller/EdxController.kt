package soaringLizards.blps.controller

import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import soaringLizards.blps.config.userDetails.JwtUserDetails
import soaringLizards.blps.dto.edx.EdxCompletionBatchRequest
import soaringLizards.blps.dto.edx.EdxEnrollmentRequest
import soaringLizards.blps.service.EdxService

@RestController
@RequestMapping("/api/edx")
@Tag(name = "Контроллер прослойка для общения с Tutor")
class EdxController(
    private val edxService: EdxService,
) {

    @PreAuthorize("hasRole('OPENEDX_USER')")
    @GetMapping("/courses")
    fun getAllCourses(
        @AuthenticationPrincipal principal: JwtUserDetails,
        @RequestParam(required = false) page: Int?,
        @RequestParam(required = false) pageSize: Int?,
    ) =
        edxService.getCourses(principal, page, pageSize)

    @PreAuthorize("hasRole('OPENEDX_USER')")
    @GetMapping("/courses/{courseKey}")
    fun getCourse(
        @AuthenticationPrincipal principal: JwtUserDetails,
        @PathVariable courseKey: String,
    ) =
        edxService.getCourse(principal, courseKey)

    @PreAuthorize("hasRole('OPENEDX_USER')")
    @GetMapping("/courses/{courseKey}/metadata")
    fun getCourseMetadata(
        @AuthenticationPrincipal principal: JwtUserDetails,
        @PathVariable courseKey: String,
    ) =
        edxService.getCourseMetadata(principal, courseKey)

    @PreAuthorize("hasRole('OPENEDX_USER')")
    @GetMapping("/courses/{courseKey}/outline")
    fun getCourseOutline(
        @AuthenticationPrincipal principal: JwtUserDetails,
        @PathVariable courseKey: String,
    ) =
        edxService.getCourseOutline(principal, courseKey)

    @PostMapping("/enrollment")
    fun enroll(
        @AuthenticationPrincipal principal: JwtUserDetails,
        @RequestBody request: EdxEnrollmentRequest,
    ) =
        edxService.enroll(principal, request)

    @PreAuthorize("hasRole('OPENEDX_USER')")
    @GetMapping("/courses/{courseId}/enrollment")
    fun getMyEnrollment(
        @AuthenticationPrincipal principal: JwtUserDetails,
        @PathVariable courseId: String,
    ) =
        edxService.getMyEnrollment(principal, courseId)

    @PreAuthorize("hasRole('OPENEDX_USER')")
    @GetMapping("/users/{username}/courses/{courseId}/enrollment")
    //TODO: change user to admin auth
    fun getUserEnrollment(
        @AuthenticationPrincipal principal: JwtUserDetails,
        @PathVariable courseId: String,
    ) =
        edxService.getUserEnrollment(principal, courseId)

    @PreAuthorize("hasRole('OPENEDX_USER')")
    @GetMapping("/courses/{courseKey}/progress")
    fun getProgress(
        @AuthenticationPrincipal principal: JwtUserDetails,
        @PathVariable courseKey: String,
    ) =
        edxService.getProgress(principal, courseKey)

    @PreAuthorize("hasRole('OPENEDX_USER')")
    @GetMapping("/courses/{courseKey}/progress/{studentId}")
    //TODO: change user to admin auth
    fun getProgressForStudent(
        @AuthenticationPrincipal principal: JwtUserDetails,
        @PathVariable courseKey: String,
        @PathVariable studentId: String,
    ) =
        edxService.getProgress(principal, courseKey, studentId)

    @PreAuthorize("hasRole('OPENEDX_USER')")
    @PostMapping("/completion-batch")
    fun trackCompletion(
        @AuthenticationPrincipal principal: JwtUserDetails,
        @RequestBody request: EdxCompletionBatchRequest,
    ) =
        edxService.trackCompletion(principal, request)

    @PreAuthorize("hasRole('OPENEDX_USER')")
    @GetMapping("/users/{username}/courses/{courseKey}/subsections/{subsectionId}/completion")
    fun getSubsectionCompletion(
        @AuthenticationPrincipal principal: JwtUserDetails,
        @PathVariable courseKey: String,
        @PathVariable subsectionId: String,
    ) =
        edxService.getSubsectionCompletion(principal, courseKey, subsectionId)
}
