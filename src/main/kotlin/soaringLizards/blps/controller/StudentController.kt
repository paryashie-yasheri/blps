package soaringLizards.blps.controller

import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import soaringLizards.blps.config.JwtUserDetails
import soaringLizards.blps.EdxService
import soaringLizards.blps.dto.edx.EdxCompletionBatchRequest
import soaringLizards.blps.dto.edx.EdxEnrollmentRequest
import soaringLizards.blps.dto.userAchievements.UserAchievementWithDetailsDto
import soaringLizards.blps.service.AchievementContext
import soaringLizards.blps.service.AchievementService
import soaringLizards.blps.service.RewardService
import soaringLizards.blps.service.UserBalanceService
import soaringLizards.blps.service.UserService
import java.util.UUID

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
    @GetMapping("/courses")
    fun getCourses(
        @RequestParam(required = false) page: Int?,
        @RequestParam(required = false) pageSize: Int?,
    ) = ResponseEntity.ok(edxService.getCourses(page, pageSize))

    @GetMapping("/courses/{courseKey}")
    fun getCourse(@PathVariable courseKey: String) = ResponseEntity.ok(edxService.getCourse(courseKey))

    @GetMapping("/courses/{courseKey}/metadata")
    fun getCourseMetadata(@PathVariable courseKey: String) = ResponseEntity.ok(edxService.getCourseMetadata(courseKey))

    @GetMapping("/courses/{courseKey}/outline")
    fun getCourseOutline(@PathVariable courseKey: String) = ResponseEntity.ok(edxService.getCourseOutline(courseKey))

    @PostMapping("/courses/{courseId}/enroll")
    fun enroll(
        @AuthenticationPrincipal currentUser: JwtUserDetails?,
        @PathVariable courseId: String,
    ) {
        val user = userService.getUserByUsername(currentUser!!.username)
            ?: throw AccessDeniedException("User not found")
        edxService.enroll(
            EdxEnrollmentRequest(
                courseId = courseId,
                courseDetails = null,
                mode = "audit",
                user = user.username,
            ),
        )
    }

    @PostMapping("/lessons/{courseKey}/{blockId}/complete")
    fun completeLesson(
        @AuthenticationPrincipal currentUser: JwtUserDetails?,
        @PathVariable courseKey: String,
        @PathVariable blockId: String,
    ): LessonCompleteResponse {
        val user = userService.getUserByUsername(currentUser!!.username)
            ?: throw AccessDeniedException("User not found")

        edxService.trackCompletion(
            EdxCompletionBatchRequest(
                username = user.username!!,
                courseKey = courseKey,
                blocks = mapOf(blockId to 1.0),
            ),
        )

        val awardedAchievements = achievementService.checkAndAwardAchievements(
            user.id!!,
            AchievementContext(
                type = "lesson_complete",
                courseKey = courseKey,
                blockId = blockId,
                completion = 1.0,
            ),
        )

        val newBalance = userBalanceService.getBalanceByUsername(currentUser.username)

        return LessonCompleteResponse(
            awardedAchievements = awardedAchievements.map {
                AwardedAchievementDto(
                    achievementTypeId = it.achievementTypeId.toString(),
                    code = it.code,
                    name = it.name,
                    pointsAwarded = it.pointsAwarded,
                )
            },
            currentBalance = newBalance,
        )
    }

    @GetMapping("/balance")
    fun getBalance(@AuthenticationPrincipal currentUser: JwtUserDetails?) =
        userBalanceService.getBalanceByUsername(currentUser!!.username)

    @GetMapping("/achievements")
    fun getAchievements(@AuthenticationPrincipal currentUser: JwtUserDetails?): List<UserAchievementWithDetailsDto> {
        val user = userService.getUserByUsername(currentUser!!.username)
            ?: throw AccessDeniedException("User not found")
        return achievementService.getUserAchievements(user.id!!)
    }

    @GetMapping("/rewards")
    fun getAvailableRewards() = ResponseEntity.ok(rewardService.getAvailableRewards())

    @GetMapping("/rewards/my")
    fun getMyRewards(@AuthenticationPrincipal currentUser: JwtUserDetails?): List<soaringLizards.blps.dto.rewards.UserRewardDto> {
        val user = userService.getUserByUsername(currentUser!!.username)
            ?: throw AccessDeniedException("User not found")
        return rewardService.getUserRewards(user.id!!)
    }

    @PostMapping("/rewards/{rewardId}/redeem")
    fun redeemReward(
        @AuthenticationPrincipal currentUser: JwtUserDetails?,
        @PathVariable rewardId: UUID,
    ): soaringLizards.blps.dto.rewards.UserRewardDto {
        val user = userService.getUserByUsername(currentUser!!.username)
            ?: throw AccessDeniedException("User not found")
        return rewardService.redeemReward(user.id!!, rewardId)
    }
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