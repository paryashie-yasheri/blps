package soaringLizards.blps.controller

import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import soaringLizards.blps.dto.achievementTypes.AchievementTypeDto
import soaringLizards.blps.dto.user.RegisterDto
import soaringLizards.blps.service.AchievementTypeService
import java.util.UUID

@RestController
@RequestMapping("/api/auth/achievement-type")
class AchievementTypeController(
    private val achievementTypeService: AchievementTypeService,
) {
    @GetMapping("/{id}")
    fun getById(@RequestParam id: UUID) =
        achievementTypeService.getAchievementTypeById(id)

    @PostMapping("/")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    fun addAchievementType(@RequestBody achievementTypeDto: AchievementTypeDto) =
        achievementTypeService.addAchievementType(achievementTypeDto)

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    fun deleteAchievementType(@RequestParam id: UUID) =
        achievementTypeService.deleteAchievementType(id)
}
