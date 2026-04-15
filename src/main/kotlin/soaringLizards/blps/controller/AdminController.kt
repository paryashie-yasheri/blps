package soaringLizards.blps.controller

import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import soaringLizards.blps.dto.achievementTypes.AchievementTypeDto
import soaringLizards.blps.dto.rewards.RewardCreateRequest
import soaringLizards.blps.dto.rewards.RewardDto
import soaringLizards.blps.service.AchievementTypeService
import soaringLizards.blps.service.RewardService

@RestController
@RequestMapping("/api/admin")
class AdminController(
    private val achievementTypeService: AchievementTypeService,
    private val rewardService: RewardService,
) {
    @PostMapping("/achievement-types")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    fun addAchievementType(@RequestBody dto: AchievementTypeDto) =
        achievementTypeService.addAchievementType(dto)

    @GetMapping("/achievement-types")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    fun getAllAchievementTypes() =
        achievementTypeService.getAllAchievementTypes()

    @PostMapping("/rewards")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    fun addReward(@RequestBody request: RewardCreateRequest) {
        rewardService.createReward(request.name, request.description, request.pointsCost, request.isActive, request.quantity)
    }

    @GetMapping("/rewards")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    fun getAllRewards(): List<RewardDto> = rewardService.getAllRewards()
}
