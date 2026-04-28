package soaringLizards.blps.controller

import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import soaringLizards.blps.config.userDetails.JwtUserDetails
import soaringLizards.blps.dto.edx.EdxCompletionBatchRequest
import soaringLizards.blps.dto.edx.EdxEnrollmentRequest
import soaringLizards.blps.dto.userAchievements.UserAchievementWithDetailsDto
import soaringLizards.blps.service.*
import java.util.*

@Tag(name = "Студент - управление курсами, достижениями и наградами")
@RestController
@RequestMapping("/api/student")
class StudentController(
    private val edxService: EdxService,
    private val userBalanceService: UserBalanceService,
    private val achievementService: AchievementService,
    private val rewardService: RewardService,
    private val userService: UserService,
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
    ) {
        edxService.enroll(
            currentUser,
            EdxEnrollmentRequest(
                courseId = courseId,
                courseDetails = null,
                mode = "audit",
                user = currentUser.username,
            ),
        )
    }

    @PreAuthorize("hasRole('OPENEDX_USER')")
    @PostMapping("/lessons/{courseKey}/{blockId}/complete")
    fun completeLesson(
        @AuthenticationPrincipal currentUser: JwtUserDetails,
        @PathVariable courseKey: String,
        @PathVariable blockId: String,
    ): LessonCompleteResponse {
        edxService.trackCompletion(
            currentUser,
            EdxCompletionBatchRequest(
                username = currentUser.username,
                courseKey = courseKey,
                blocks = mapOf(blockId to 1.0),
            ),
        )

        val awardedAchievements = achievementService.checkAndAwardAchievements(
            currentUser.id,
            AchievementContext(
                type = "lesson_complete",
                courseKey = courseKey,
                blockId = blockId,
                completion = 1.0,
            ),
        )

        val newBalance = userBalanceService.getCurrentUserBalance(currentUser.id)

        return LessonCompleteResponse(
            awardedAchievements = awardedAchievements.map {
                AwardedAchievementDto(
                    achievementTypeId = it.achievementTypeId.toString(),
                    code = it.code,
                    name = it.name,
                    pointsAwarded = it.pointsAwarded,
                )
            },
            currentBalance = newBalance.balance,
        )
    }

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

data class LessonCompleteResponse(
    val awardedAchievements: List<AwardedAchievementDto>,
    val currentBalance: Int,
)

data class AwardedAchievementDto(
    val achievementTypeId: String,
    val code: String,
    val name: String,
    val pointsAwarded: Int,
)
