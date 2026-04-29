package soaringLizards.blps.service

import org.springframework.security.access.AccessDeniedException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import soaringLizards.blps.dto.balanceTransactions.BalanceTransactionDto
import soaringLizards.blps.dto.ledger.LedgerEventDto
import soaringLizards.blps.dto.rewards.RewardDto
import soaringLizards.blps.dto.rewards.UserRewardDto
import soaringLizards.blps.dto.rewards.UserRewardWithBalanceDto
import soaringLizards.blps.exception.BusinessLogicException
import soaringLizards.blps.repository.RewardRepository
import soaringLizards.blps.repository.UserBalanceRepository
import soaringLizards.blps.repository.UserRewardRepository
import jakarta.transaction.TransactionManager
import org.slf4j.LoggerFactory
import java.time.OffsetDateTime
import java.util.*

@Service
class RewardService(
    private val rewardRepository: RewardRepository,
    private val userRewardRepository: UserRewardRepository,
    private val userBalanceRepository: UserBalanceRepository,
    private val s3Service: S3Service,
    private val transactionService: TransactionService,
    private val ledgerEventService: LedgerEventService,
    private val transactionManager: TransactionManager,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    fun getAvailableRewards(): List<RewardDto> {
        val rewards = rewardRepository.findAllActive()
        return rewards.map { it.toRewardDto() }
    }

    fun getUserRewards(userId: UUID): List<UserRewardDto> {
        val balance = userBalanceRepository.findByUserId(userId)
            ?: throw AccessDeniedException("Balance not found")

        val userRewards = userRewardRepository.findByBalanceId(balance.balanceId!!)
        return userRewards.map { it.toUserRewardDto() }
    }

    @Transactional
    fun redeemReward(userId: UUID, rewardId: UUID): UserRewardDto {
        val operationId = UUID.randomUUID()
        val xid = transactionManager.transaction?.toString()
        logger.info("redeem_reward_started operationId={} xid={} userId={} rewardId={}", operationId, xid, userId, rewardId)
        try {
            val balance = userBalanceRepository.findByUserId(userId)
                ?: throw BusinessLogicException("User balance not found")

            val reward = rewardRepository.findById(rewardId)
                ?: throw BusinessLogicException("Reward not found")

            if (reward.isActive != true) {
                throw BusinessLogicException("Reward is not available")
            }

            val pointsCost = reward.pointsCost ?: 0
            if (balance.currentBalance!! < pointsCost) {
                throw BusinessLogicException("Insufficient balance")
            }

            if (reward.quantity != null && reward.quantity!! <= 0) {
                throw BusinessLogicException("Reward out of stock")
            }

            val newBalance = balance.currentBalance - pointsCost
            val newTotalSpent = balance.totalSpent!! + pointsCost

            userBalanceRepository.update(
                balance.balanceId!!,
                newBalance,
                balance.totalEarned!!,
                newTotalSpent,
                balance.version!! + 1,
            )

            val transaction = BalanceTransactionDto(
                balanceId = balance.balanceId,
                transactionType = "REWARD_REDEEM",
                amount = -pointsCost,
                balanceBefore = balance.currentBalance,
                balanceAfter = newBalance,
                userAchievementId = null,
                achievementTypeId = null,
                description = "Redeemed reward: ${reward.name}",
                createdAt = OffsetDateTime.now(),
            )
            transactionService.addTransaction(transaction)
            ledgerEventService.append(
                LedgerEventDto(
                    operationId = operationId,
                    eventType = "REWARD_REDEEM",
                    occurredAt = OffsetDateTime.now(),
                    userId = userId,
                    balanceId = balance.balanceId,
                    amount = -pointsCost,
                    balanceBefore = balance.currentBalance,
                    balanceAfter = newBalance,
                    rewardId = rewardId,
                    description = "Redeemed reward: ${reward.name}",
                ),
            )

            userRewardRepository.insert(balance.balanceId, rewardId)

            if (reward.quantity != null) {
                rewardRepository.decrementQuantity(rewardId)
            }

            return UserRewardDto(
                userRewardId = null,
                reward = reward.toRewardDto(),
                redeemedAt = OffsetDateTime.now().toString(),
            )
        } catch (ex: Exception) {
            logger.error(
                "redeem_reward_failed operationId={} xid={} userId={} rewardId={}",
                operationId,
                xid,
                userId,
                rewardId,
                ex,
            )
            throw ex
        }
    }

    private fun soaringLizards.blps.jooq.generated.tables.records.RewardsRecord.toRewardDto(): RewardDto {
        return RewardDto(
            rewardId = this.rewardId?.toString(),
            name = this.name ?: "",
            description = this.description,
            pointsCost = this.pointsCost ?: 0,
            iconUrl = this.iconKey?.let { s3Service.get("rewards/$it") },
            isActive = this.isActive ?: false,
            quantity = this.quantity,
        )
    }

    private fun UserRewardWithBalanceDto.toUserRewardDto(): UserRewardDto {
        return UserRewardDto(
            userRewardId = this.userRewardId,
            reward = RewardDto(
                rewardId = this.rewardId,
                name = this.rewardName ?: "",
                description = this.rewardDescription,
                pointsCost = this.rewardPointsCost ?: 0,
                iconUrl = this.rewardIconUrl?.let { s3Service.get("rewards/$it") },
                isActive = true,
                quantity = null,
            ),
            redeemedAt = this.redeemedAt ?: "",
        )
    }
}
