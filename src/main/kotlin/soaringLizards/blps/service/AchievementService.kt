package soaringLizards.blps.service

import org.springframework.security.access.AccessDeniedException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import soaringLizards.blps.dto.balanceTransactions.BalanceTransactionDto
import soaringLizards.blps.dto.userAchievements.UserAchievementDto
import soaringLizards.blps.dto.userAchievements.UserAchievementWithDetailsDto
import soaringLizards.blps.entity.AchievementCondition
import soaringLizards.blps.entity.AchievementType
import soaringLizards.blps.entity.UserBalance
import soaringLizards.blps.exception.BusinessLogicException
import soaringLizards.blps.repository.UserAchievementRepository
import soaringLizards.blps.repository.UserBalanceRepository
import java.time.OffsetDateTime
import java.util.*

data class AchievementContext(
    val type: String,
    val courseKey: String,
    val blockId: String?,
    val completion: Double?,
)

data class AwardedAchievement(
    val achievementTypeId: UUID,
    val code: String,
    val name: String,
    val pointsAwarded: Int,
)

@Service
class AchievementService(
    private val userAchievementRepository: UserAchievementRepository,
    private val userBalanceRepository: UserBalanceRepository,
    private val transactionService: TransactionService,
    private val achievementTypeService: AchievementTypeService,
    private val s3Service: S3Service,
) {
    @Transactional
    fun checkAndAwardAchievements(
        userId: UUID,
        context: AchievementContext,
    ): List<AwardedAchievement> {
        val balance = userBalanceRepository.findByUserId(userId)
            ?: throw BusinessLogicException("User balance not found")

        val achievementTypes = achievementTypeService.getActiveAchievementTypesForAwarding()

        val awardedAchievements = mutableListOf<AwardedAchievement>()

        for (type in achievementTypes) {
            if (!matchesConditions(type, context)) continue

            if (type.isRepeatable != true) {
                val existing = userAchievementRepository.findByBalanceAndType(
                    balance.balanceId!!,
                    type.achievementTypeId!!,
                    null,
                )
                if (existing.isNotEmpty()) continue
            }

            userAchievementRepository.insert(
                UserAchievementDto(
                    balanceId = balance.balanceId!!,
                    achievementTypeId = type.achievementTypeId!!,
                    earnedAt = OffsetDateTime.now(),
                    referenceId = null,
                    referenceType = context.type,
                    isClaimed = false,
                ),
            )

            val balanceDiff = type.pointsAwarded ?: 0
            if (balanceDiff > 0) {
                addBalancePoints(balance, balanceDiff)
            }

            awardedAchievements.add(
                AwardedAchievement(
                    achievementTypeId = type.achievementTypeId,
                    code = type.code!!,
                    name = type.name!!,
                    pointsAwarded = type.pointsAwarded ?: 0,
                ),
            )
        }

        return awardedAchievements
    }

    private fun matchesConditions(
        type: AchievementType,
        context: AchievementContext,
    ) = if (type.conditions.isEmpty())
        false
    else type.conditions.all { condition -> matchesCondition(condition, context) }

    private fun matchesCondition(
        condition: AchievementCondition,
        context: AchievementContext,
    ): Boolean {
        if (condition.eventType != context.type) {
            return false
        }

        if (condition.coursePattern != null && !matchGlob(condition.coursePattern, context.courseKey)) {
            return false
        }

        if (condition.blockPattern != null) {
            val blockId = context.blockId ?: return false
            if (!matchGlob(condition.blockPattern, blockId)) {
                return false
            }
        }

        if (condition.minCompletion != null) {
            val completion = context.completion ?: return false
            if (completion < condition.minCompletion) {
                return false
            }
        }

        return true
    }

    private fun matchGlob(pattern: String, text: String): Boolean {
        val regex = buildString {
            pattern.forEach { char ->
                if (char == '*') {
                    append(".*")
                } else {
                    append(Regex.escape(char.toString()))
                }
            }
        }
        return Regex("^$regex$").matches(text)
    }

    private fun addBalancePoints(balance: UserBalance, points: Int) {
        val newBalance = balance.currentBalance!! + points
        val newTotalEarned = balance.totalEarned!! + points

        userBalanceRepository.update(
            balance.balanceId!!,
            newBalance,
            newTotalEarned,
            balance.totalSpent!!,
            balance.version!! + 1,
        )

        val transaction = BalanceTransactionDto(
            balanceId = balance.balanceId,
            transactionType = "ACHIEVEMENT_REWARD",
            amount = points,
            balanceBefore = balance.currentBalance,
            balanceAfter = newBalance,
            userAchievementId = null,
            achievementTypeId = null,
            description = "Achievement reward",
            createdAt = OffsetDateTime.now(),
        )
        transactionService.addTransaction(transaction)
    }

    fun getUserAchievements(userId: UUID): List<UserAchievementWithDetailsDto> {
        val balance = userBalanceRepository.findByUserId(userId)
            ?: throw AccessDeniedException("Balance not found")
        return userAchievementRepository.findByBalanceId(balance.balanceId!!)
            .map { achievement ->
                achievement.copy(
                    iconUrl = achievement.icon?.let { s3Service.get(it) },
                )
            }
    }
}
