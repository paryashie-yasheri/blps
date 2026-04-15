package soaringLizards.blps.controller

import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/achievements")
class AchievementController() {
    
    @GetMapping("/available")
    @PreAuthorize("hasRole('STAFF')")
    fun getAvailableAchievements() =
        mapOf(
            "achievements" to listOf(
                mapOf(
                    "id" to "achievement-1",
                    "name" to "First Course Completion",
                    "description" to "Complete your first course",
                    "points" to 100,
                    "icon" to "trophy",
                    "claimed" to false
                ),
                mapOf(
                    "id" to "achievement-2",
                    "name" to "Speed Learner",
                    "description" to "Complete a course in under 24 hours",
                    "points" to 250,
                    "icon" to "lightning",
                    "claimed" to true
                )
            )
        )
}