package soaringLizards.blps

import org.assertj.core.api.Assertions.assertThat
import org.jooq.DSLContext
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import soaringLizards.blps.integration.IntegrationPostgres
import soaringLizards.blps.integration.TestcontainersCleanupExtension
import soaringLizards.blps.jooq.generated.tables.references.REWARDS
import soaringLizards.blps.jooq.generated.tables.references.USER_REWARDS
import soaringLizards.blps.repository.RewardRepository
import soaringLizards.blps.repository.UserRewardRepository
import soaringLizards.blps.repository.UserBalanceRepository

@SpringBootTest
@ExtendWith(TestcontainersCleanupExtension::class)
class RewardsTwoPcRollbackTest {

    @Autowired
    private lateinit var rewardRepository: RewardRepository

    @Autowired
    private lateinit var userRewardRepository: UserRewardRepository

    @Autowired
    private lateinit var userBalanceRepository: UserBalanceRepository

    @Autowired
    private lateinit var mainDsl: DSLContext

    @Autowired
    @Qualifier("rewardsDslContext")
    private lateinit var rewardsDsl: DSLContext

    companion object {
        @JvmStatic
        @DynamicPropertySource
        fun registerProperties(registry: DynamicPropertyRegistry) {
            IntegrationPostgres.registerProperties(registry)
        }
    }

    @AfterEach
    fun cleanup() {
        rewardsDsl.deleteFrom(USER_REWARDS).execute()
        rewardsDsl.deleteFrom(REWARDS).execute()
    }

    @Test
    fun `rewards DB is separate from main DB`() {
        val beforeCount = rewardsDsl.fetchCount(REWARDS)

        val rewardId = rewardsDsl.insertInto(REWARDS)
            .set(REWARDS.NAME, "test-reward")
            .set(REWARDS.DESCRIPTION, "test")
            .set(REWARDS.POINTS_COST, 50)
            .set(REWARDS.IS_ACTIVE, true)
            .set(REWARDS.QUANTITY, 10)
            .returningResult(REWARDS.REWARD_ID)
            .fetchOne(REWARDS.REWARD_ID)!!

        val fetchedReward = rewardRepository.findById(rewardId)
        assertThat(fetchedReward).isNotNull
        assertThat(fetchedReward!!.name).isEqualTo("test-reward")

        assertThat(rewardsDsl.fetchCount(REWARDS)).isEqualTo(beforeCount + 1)
    }
}
