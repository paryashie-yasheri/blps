package soaringLizards.blps.controller

import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile
import soaringLizards.blps.dto.achievementTypes.AchievementTypeUpsertRequest
import soaringLizards.blps.service.AchievementTypeService
import java.util.UUID

@RestController
@RequestMapping("/api/achievements")
@PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
class AchievementController(
    private val achievementTypeService: AchievementTypeService,
) {
    @GetMapping
    fun getAchievementTypes() = achievementTypeService.listAchievementTypes()

    @GetMapping("/{id}")
    fun getAchievementType(@PathVariable("id") id: UUID) =
        achievementTypeService.getAchievementTypeById(id)

    @PostMapping(consumes = ["multipart/form-data"])
    fun createAchievementType(
        @Valid @RequestPart("request") request: AchievementTypeUpsertRequest,
        @RequestPart("image", required = false) image: MultipartFile?,
    ) = ResponseEntity
        .status(HttpStatus.CREATED)
        .body(achievementTypeService.createAchievementType(request, image))

    @PutMapping("/{id}", consumes = ["multipart/form-data"])
    fun updateAchievementType(
        @PathVariable("id") id: UUID,
        @Valid @RequestPart("request") request: AchievementTypeUpsertRequest,
        @RequestPart("image", required = false) image: MultipartFile?,
    ) = achievementTypeService.updateAchievementType(id, request, image)

    @DeleteMapping("/{id}")
    fun deleteAchievementType(@PathVariable("id") id: UUID): ResponseEntity<Void> {
        achievementTypeService.deleteAchievementType(id)
        return ResponseEntity.noContent().build()
    }
}
