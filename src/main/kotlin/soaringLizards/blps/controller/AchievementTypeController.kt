package soaringLizards.blps.controller

import jakarta.websocket.server.PathParam
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import soaringLizards.blps.dto.achievementTypes.AchievementTypeDto
import soaringLizards.blps.service.AchievementTypeService
import java.util.*

@RestController
@RequestMapping("/api/auth/achievement-type")
class AchievementTypeController(
    private val achievementTypeService: AchievementTypeService,
) {
    @GetMapping("/{id}")
    fun getById(@PathParam("id") id: UUID) =
        achievementTypeService.getAchievementTypeById(id)

    @PostMapping("/")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    fun addAchievementType(@RequestBody achievementTypeDto: AchievementTypeDto) =
        achievementTypeService.addAchievementType(achievementTypeDto)

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    fun deleteAchievementType(@PathParam("id") id: UUID) =
        achievementTypeService.deleteAchievementType(id)
}
