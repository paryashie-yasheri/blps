package soaringLizards.blps.service

import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import soaringLizards.blps.entity.AchievementCondition
import soaringLizards.blps.entity.AchievementType
import soaringLizards.blps.entity.UserAchievement
import soaringLizards.blps.entity.UserBalance
import soaringLizards.blps.repository.UserAchievementRepository
import soaringLizards.blps.repository.UserBalanceRepository
import java.time.OffsetDateTime
import java.util.UUID
import kotlin.test.assertEquals

class AchievementServiceTest {
    private val userAchievementRepository = mock<UserAchievementRepository>()
    private val userBalanceRepository = mock<UserBalanceRepository>()
    private val transactionService = mock<TransactionService>()
    private val achievementTypeService = mock<AchievementTypeService>()
    private val s3Service = mock<S3Service>()

    private val service = AchievementService(
        userAchievementRepository = userAchievementRepository,
        userBalanceRepository = userBalanceRepository,
        transactionService = transactionService,
        achievementTypeService = achievementTypeService,
        s3Service = s3Service,
    )

    @Test
    fun `lesson_complete matching awards non-repeatable achievement only once`() {
        val balanceId = UUID.randomUUID()
        val achievementTypeId = UUID.randomUUID()
        val userId = UUID.randomUUID()

        whenever(userBalanceRepository.findByUserId(userId)).thenReturn(
            UserBalance(
                balanceId = balanceId,
                userId = userId,
                currentBalance = 100,
                totalEarned = 100,
                totalSpent = 0,
                version = 1,
            ),
        )
        whenever(achievementTypeService.getActiveAchievementTypesForAwarding()).thenReturn(
            listOf(
                AchievementType(
                    achievementTypeId = achievementTypeId,
                    code = "lesson_master",
                    name = "Lesson Master",
                    description = "Finish intro lesson",
                    pointsAwarded = 25,
                    iconKey = "achievements/icon.png",
                    isRepeatable = false,
                    isActive = true,
                    conditions = listOf(
                        AchievementCondition(
                            conditionId = UUID.randomUUID(),
                            achievementTypeId = achievementTypeId,
                            eventType = "lesson_complete",
                            coursePattern = "course-v1:ITMO+*",
                            blockPattern = "block-v1:*intro*",
                            minCompletion = 1.0,
                        ),
                    ),
                ),
            ),
        )
        whenever(userAchievementRepository.findByBalanceAndType(eq(balanceId), eq(achievementTypeId), eq(null)))
            .thenReturn(
                emptyList(),
                listOf(
                    UserAchievement(
                        userAchievementId = UUID.randomUUID(),
                        balanceId = balanceId,
                        achievementTypeId = achievementTypeId,
                        earnedAt = OffsetDateTime.now(),
                        referenceId = null,
                        referenceType = "lesson_complete",
                        isClaimed = false,
                    ),
                ),
            )

        val firstAward = service.checkAndAwardAchievements(
            userId = userId,
            context = AchievementContext(
                type = "lesson_complete",
                courseKey = "course-v1:ITMO+Kotlin",
                blockId = "block-v1:ITMO+type@vertical+block@intro",
                completion = 1.0,
            ),
        )
        val secondAward = service.checkAndAwardAchievements(
            userId = userId,
            context = AchievementContext(
                type = "lesson_complete",
                courseKey = "course-v1:ITMO+Kotlin",
                blockId = "block-v1:ITMO+type@vertical+block@intro",
                completion = 1.0,
            ),
        )

        assertEquals(1, firstAward.size)
        assertEquals(0, secondAward.size)
        verify(userAchievementRepository, times(1)).insert(any())
        verify(userBalanceRepository, times(1)).update(eq(balanceId), eq(125), eq(125), eq(0), eq(2))
        verify(transactionService, times(1)).addTransaction(any())
    }

    @Test
    fun `achievement type without conditions is not awarded`() {
        val balanceId = UUID.randomUUID()
        val achievementTypeId = UUID.randomUUID()
        val userId = UUID.randomUUID()

        whenever(userBalanceRepository.findByUserId(userId)).thenReturn(
            UserBalance(
                balanceId = balanceId,
                userId = userId,
                currentBalance = 0,
                totalEarned = 0,
                totalSpent = 0,
                version = 1,
            ),
        )
        whenever(achievementTypeService.getActiveAchievementTypesForAwarding()).thenReturn(
            listOf(
                AchievementType(
                    achievementTypeId = achievementTypeId,
                    code = "empty_conditions",
                    name = "Empty Conditions",
                    description = "Should not be awarded",
                    pointsAwarded = 10,
                    iconKey = null,
                    isRepeatable = false,
                    isActive = true,
                    conditions = emptyList(),
                ),
            ),
        )

        val awarded = service.checkAndAwardAchievements(
            userId = userId,
            context = AchievementContext(
                type = "lesson_complete",
                courseKey = "course-v1:ITMO+Kotlin",
                blockId = "block-v1:ITMO+type@vertical+block@intro",
                completion = 1.0,
            ),
        )

        assertEquals(0, awarded.size)
        verify(userAchievementRepository, never()).insert(any())
        verify(userBalanceRepository, never()).update(any(), any(), any(), any(), any())
        verify(transactionService, never()).addTransaction(any())
    }
}
