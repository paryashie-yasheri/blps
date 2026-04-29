package soaringLizards.blps.controller

import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import soaringLizards.blps.config.userDetails.JwtUserDetails
import soaringLizards.blps.dto.student.CourseEnrollResponse
import soaringLizards.blps.dto.student.LessonCompleteResponse
import soaringLizards.blps.dto.userAchievements.UserAchievementWithDetailsDto
import soaringLizards.blps.service.AchievementService
import soaringLizards.blps.service.EdxService
import soaringLizards.blps.service.RewardService
import soaringLizards.blps.service.StudentAchievementWorkflowService
import soaringLizards.blps.service.UserBalanceService
import java.util.*

@Tag(name = "Студент - управление курсами, достижениями и наградами")
@RestController
@RequestMapping("/api/student")
class StudentController(
    private val edxService: EdxService,
    private val studentAchievementWorkflowService: StudentAchievementWorkflowService,
    private val userBalanceService: UserBalanceService,
    private val achievementService: AchievementService,
    private val rewardService: RewardService,
) {
    @PreAuthorize("hasRole('OPENEDX_USER')")
    @GetMapping("/courses")
    fun getCourses(
        @AuthenticationPrincipal currentUser: JwtUserDetails,
        @RequestParam(required = false) page: Int?,
        @RequestParam(required = false) pageSize: Int?,
    ) = edxService.getCourses(currentUser, page, pageSize)

    @PreAuthorize("hasRole('OPENEDX_USER')")
    @GetMapping("/courses/{courseKey}")
    fun getCourse(@AuthenticationPrincipal currentUser: JwtUserDetails, @PathVariable courseKey: String) =
        edxService.getCourse(currentUser, courseKey)

    @PreAuthorize("hasRole('OPENEDX_USER')")
    @GetMapping("/courses/{courseKey}/metadata")
    fun getCourseMetadata(@AuthenticationPrincipal currentUser: JwtUserDetails, @PathVariable courseKey: String) =
        edxService.getCourseMetadata(currentUser, courseKey)

    @PreAuthorize("hasRole('OPENEDX_USER')")
    @GetMapping("/courses/{courseKey}/outline")
    fun getCourseOutline(@AuthenticationPrincipal currentUser: JwtUserDetails, @PathVariable courseKey: String) =
        edxService.getCourseOutline(currentUser, courseKey)

    @PreAuthorize("hasRole('OPENEDX_USER')")
    @PostMapping("/courses/{courseId}/enroll")
    fun enroll(
        @AuthenticationPrincipal currentUser: JwtUserDetails,
        @PathVariable courseId: String,
    ): CourseEnrollResponse =
        studentAchievementWorkflowService.enroll(currentUser, courseId)

    @PreAuthorize("hasRole('OPENEDX_USER')")
    @PostMapping("/lessons/{courseKey}/{blockId}/complete")
    fun completeLesson(
        @AuthenticationPrincipal currentUser: JwtUserDetails,
        @PathVariable courseKey: String,
        @PathVariable blockId: String,
    ): LessonCompleteResponse =
        studentAchievementWorkflowService.completeLesson(currentUser, courseKey, blockId)

    @PreAuthorize("hasRole('OPENEDX_USER')")
    @GetMapping("/balance")
    fun getBalance(@AuthenticationPrincipal currentUser: JwtUserDetails) =
        userBalanceService.getCurrentUserBalance(currentUser.id)

    @PreAuthorize("hasRole('OPENEDX_USER')")
    @GetMapping("/achievements")
    fun getAchievements(@AuthenticationPrincipal currentUser: JwtUserDetails): List<UserAchievementWithDetailsDto> =
        achievementService.getUserAchievements(currentUser.id)

    @PreAuthorize("hasRole('OPENEDX_USER')")
    @GetMapping("/rewards")
    fun getAvailableRewards() = rewardService.getAvailableRewards()

    @PreAuthorize("hasRole('OPENEDX_USER')")
    @GetMapping("/rewards/my")
    fun getMyRewards(@AuthenticationPrincipal currentUser: JwtUserDetails) =
        rewardService.getUserRewards(currentUser.id)

    @PreAuthorize("hasRole('OPENEDX_USER')")
    @PostMapping("/rewards/{rewardId}/redeem")
    fun redeemReward(
        @AuthenticationPrincipal currentUser: JwtUserDetails,
        @PathVariable rewardId: UUID,
    ) = rewardService.redeemReward(currentUser.id, rewardId)
}
