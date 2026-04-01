package soaringLizards.blps.service

import org.springframework.stereotype.Service
import soaringLizards.blps.dto.achievementTypes.AchievementTypeDto
import soaringLizards.blps.repository.AchievementTypeRepository
import java.util.*

@Service
class AchievementTypeService(
    private val achievementTypeRepository: AchievementTypeRepository,
) {
    fun addAchievementType(achievementDto: AchievementTypeDto) =
        achievementTypeRepository.insert(achievementDto)

    fun deleteAchievementType(achievementTypeId: UUID) =
        achievementTypeRepository.delete(achievementTypeId)

    fun getAchievementTypeById(achievementTypeId: UUID) =
        achievementTypeRepository.findById(achievementTypeId)

}
