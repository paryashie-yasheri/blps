package soaringLizards.blps.service

import org.springframework.stereotype.Service
import org.springframework.transaction.support.TransactionTemplate
import org.springframework.web.multipart.MultipartFile
import soaringLizards.blps.dto.achievementTypes.AchievementConditionRequest
import soaringLizards.blps.dto.achievementTypes.AchievementConditionResponse
import soaringLizards.blps.dto.achievementTypes.AchievementTypeResponse
import soaringLizards.blps.dto.achievementTypes.AchievementTypeUpsertRequest
import soaringLizards.blps.entity.AchievementCondition
import soaringLizards.blps.entity.AchievementType
import soaringLizards.blps.exception.BusinessLogicException
import soaringLizards.blps.exception.ResourceNotFoundException
import soaringLizards.blps.repository.AchievementConditionRepository
import soaringLizards.blps.repository.AchievementTypeRepository
import java.util.UUID

@Service
class AchievementTypeService(
    private val achievementTypeRepository: AchievementTypeRepository,
    private val achievementConditionRepository: AchievementConditionRepository,
    private val s3Service: S3Service,
    private val transactionTemplate: TransactionTemplate,
) {
    fun listAchievementTypes(): List<AchievementTypeResponse> =
        loadWithConditions(achievementTypeRepository.findAll()).map { it.toResponse() }

    fun getAchievementTypeById(achievementTypeId: UUID): AchievementTypeResponse =
        getAchievementTypeOrThrow(achievementTypeId).toResponse()

    fun getActiveAchievementTypesForAwarding(): List<AchievementType> =
        loadWithConditions(achievementTypeRepository.findAllActive())

    fun createAchievementType(
        request: AchievementTypeUpsertRequest,
        image: MultipartFile?,
    ): AchievementTypeResponse {
        validateRequest(request)
        val uploadedIconKey = image?.let { s3Service.uploadAchievementIcon(it) }

        return try {
            transactionTemplate.execute {
                createAchievementTypeInTransaction(request, uploadedIconKey)
            }.toResponse()
        } catch (e: Exception) {
            if (uploadedIconKey != null)
                runCatching { s3Service.delete(uploadedIconKey) }
            throw e
        }
    }

    fun updateAchievementType(
        achievementTypeId: UUID,
        request: AchievementTypeUpsertRequest,
        image: MultipartFile?,
    ): AchievementTypeResponse {
        validateRequest(request)
        val existing = getAchievementTypeOrThrow(achievementTypeId)
        val uploadedIconKey = image?.let { s3Service.uploadAchievementIcon(it) }
        val iconKeyToPersist = uploadedIconKey ?: existing.iconKey

        return try {
            val updated = transactionTemplate.execute {
                updateAchievementTypeInTransaction(
                    achievementTypeId = achievementTypeId,
                    request = request,
                    iconKey = iconKeyToPersist,
                )
            }.toResponse()

            if (uploadedIconKey != null && existing.iconKey != null && existing.iconKey != uploadedIconKey)
                runCatching { s3Service.delete(existing.iconKey) }

            updated
        } catch (ex: Exception) {
            if (uploadedIconKey != null)
                runCatching { s3Service.delete(uploadedIconKey) }
            throw ex
        }
    }

    fun deleteAchievementType(achievementTypeId: UUID) {
        val existing = getAchievementTypeOrThrow(achievementTypeId)
        transactionTemplate.executeWithoutResult {
            deleteAchievementTypeInTransaction(achievementTypeId)
        }
        if (existing.iconKey != null)
            runCatching { s3Service.delete(existing.iconKey) }
    }

    private fun getAchievementTypeOrThrow(achievementTypeId: UUID): AchievementType =
        loadWithConditions(listOfNotNull(achievementTypeRepository.findById(achievementTypeId)))
            .firstOrNull()
            ?: throw ResourceNotFoundException("Achievement type not found")

    private fun loadWithConditions(types: List<AchievementType>): List<AchievementType> {
        if (types.isEmpty())
            return emptyList()

        val conditionsByTypeId =
            achievementConditionRepository.findByAchievementTypeIds(
                types.mapNotNull { it.achievementTypeId },
            )

        return types.map { type ->
            type.copy(
                conditions = type
                    .achievementTypeId
                    ?.let { conditionsByTypeId[it] }
                    .orEmpty(),
            )
        }
    }

    private fun validateRequest(request: AchievementTypeUpsertRequest) {
        if (request.conditions.isEmpty())
            throw BusinessLogicException("Achievement conditions must not be empty")

        if (request.conditions.map { it.eventType }.distinct().size > 1)
            throw BusinessLogicException("All achievement conditions must use the same event type")
    }

    private fun createAchievementTypeInTransaction(
        request: AchievementTypeUpsertRequest,
        iconKey: String?,
    ): AchievementType {
        val createdType = achievementTypeRepository.insert(request.toEntity(iconKey))
        val achievementTypeId = createdType
            .achievementTypeId
            ?: throw IllegalStateException("Achievement type ID must be generated")

        achievementConditionRepository.replaceForAchievementType(
            achievementTypeId,
            request.conditions.map { it.toEntity(achievementTypeId) },
        )

        return loadWithConditions(listOf(createdType)).first()
    }

    private fun updateAchievementTypeInTransaction(
        achievementTypeId: UUID,
        request: AchievementTypeUpsertRequest,
        iconKey: String?,
    ): AchievementType {
        val updatedType = achievementTypeRepository
            .update(
                request.toEntity(iconKey).copy(achievementTypeId = achievementTypeId),
            ) ?: throw ResourceNotFoundException("Achievement type not found")

        achievementConditionRepository.replaceForAchievementType(
            achievementTypeId,
            request.conditions.map { it.toEntity(achievementTypeId) },
        )

        return loadWithConditions(listOf(updatedType)).first()
    }

    private fun deleteAchievementTypeInTransaction(achievementTypeId: UUID) {
        val deletedCount = achievementTypeRepository.delete(achievementTypeId)
        if (deletedCount == 0) {
            throw ResourceNotFoundException("Achievement type not found")
        }
    }

    private fun AchievementType.toResponse() =
        AchievementTypeResponse(
            achievementTypeId = achievementTypeId.toString(),
            code = code.orEmpty(),
            name = name.orEmpty(),
            description = description,
            pointsAwarded = pointsAwarded ?: 0,
            iconKey = iconKey,
            iconUrl = iconKey?.let { s3Service.get(it) },
            isRepeatable = isRepeatable ?: false,
            isActive = isActive ?: false,
            conditions = conditions.map { condition ->
                AchievementConditionResponse(
                    conditionId = condition.conditionId.toString(),
                    eventType = condition.eventType,
                    coursePattern = condition.coursePattern,
                    blockPattern = condition.blockPattern,
                    minCompletion = condition.minCompletion,
                )
            },
        )

    private fun AchievementTypeUpsertRequest.toEntity(iconKey: String?) =
        AchievementType(
            achievementTypeId = null,
            code = code,
            name = name,
            description = description,
            pointsAwarded = pointsAwarded,
            iconKey = iconKey,
            isRepeatable = isRepeatable,
            isActive = isActive,
            conditions = emptyList(),
        )

    private fun AchievementConditionRequest.toEntity(achievementTypeId: UUID) =
        AchievementCondition(
            conditionId = null,
            achievementTypeId = achievementTypeId,
            eventType = eventType,
            coursePattern = coursePattern,
            blockPattern = blockPattern,
            minCompletion = minCompletion,
        )
}
