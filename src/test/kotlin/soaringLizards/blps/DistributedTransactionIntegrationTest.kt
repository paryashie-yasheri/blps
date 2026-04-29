package soaringLizards.blps

import org.assertj.core.api.Assertions.assertThat
import org.jooq.DSLContext
import org.jooq.impl.DSL.field
import org.jooq.impl.DSL.name
import org.jooq.impl.DSL.table
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import soaringLizards.blps.config.userDetails.JwtUserDetails
import soaringLizards.blps.dto.balanceTransactions.DonateRequest
import soaringLizards.blps.dto.user.Email
import soaringLizards.blps.integration.IntegrationPostgres
import soaringLizards.blps.integration.TestcontainersCleanupExtension
import soaringLizards.blps.jooq.generated.tables.references.BALANCE_TRANSACTIONS
import soaringLizards.blps.jooq.generated.tables.references.REWARDS
import soaringLizards.blps.jooq.generated.tables.references.USER_BALANCES
import soaringLizards.blps.jooq.generated.tables.references.USER_REWARDS
import soaringLizards.blps.jooq.generated.tables.references.USERS
import soaringLizards.blps.service.LedgerEventService
import soaringLizards.blps.service.RewardService
import soaringLizards.blps.service.UserBalanceService
import java.util.UUID
import kotlin.test.assertFailsWith

@SpringBootTest
@ExtendWith(TestcontainersCleanupExtension::class)
class DistributedTransactionIntegrationTest {

    @Autowired
    private lateinit var rewardService: RewardService

    @Autowired
    private lateinit var userBalanceService: UserBalanceService

    @Autowired
    private lateinit var ledgerEventService: LedgerEventService

    @Autowired
    private lateinit var mainDsl: DSLContext

    @Autowired
    @Qualifier("journalDslContext")
    private lateinit var journalDsl: DSLContext

    private val ledgerEvents = table(name("ledger_events"))

    companion object {
        @JvmStatic
        @DynamicPropertySource
        fun registerProperties(registry: DynamicPropertyRegistry) {
            IntegrationPostgres.registerProperties(registry)
        }
    }

    @AfterEach
    fun cleanup() {
        journalDsl.execute("DROP TRIGGER IF EXISTS trg_fail_ledger_insert ON ledger_events")
        journalDsl.execute("DROP FUNCTION IF EXISTS fail_ledger_insert()")
        journalDsl.execute("TRUNCATE TABLE ledger_events")
        mainDsl.deleteFrom(BALANCE_TRANSACTIONS).execute()
        mainDsl.deleteFrom(USER_REWARDS).execute()
        mainDsl.deleteFrom(REWARDS).execute()
        mainDsl.deleteFrom(USER_BALANCES).execute()
        mainDsl.deleteFrom(USERS).execute()
    }

    @Test
    fun `redeem reward writes both databases in one transaction`() {
        val userId = insertUser()
        val balanceId = insertBalance(userId, currentBalance = 120)
        val rewardId = insertReward(pointsCost = 70, quantity = 3)

        rewardService.redeemReward(userId, rewardId)

        val updatedBalance = mainDsl.selectFrom(USER_BALANCES)
            .where(USER_BALANCES.BALANCE_ID.eq(balanceId))
            .fetchOne()!!
        assertThat(updatedBalance.currentBalance).isEqualTo(50)
        assertThat(updatedBalance.totalSpent).isEqualTo(70)
        assertThat(mainDsl.fetchCount(USER_REWARDS)).isEqualTo(1)
        assertThat(mainDsl.fetchCount(BALANCE_TRANSACTIONS)).isEqualTo(1)
        assertThat(journalDsl.fetchCount(ledgerEvents)).isEqualTo(1)
    }

    @Test
    fun `donate points writes immutable event to journal db`() {
        val userId = insertUser()
        val balanceId = insertBalance(userId, currentBalance = 200)

        userBalanceService.donatePoints(testUser(userId), DonateRequest(amount = 40))

        val updatedBalance = mainDsl.selectFrom(USER_BALANCES)
            .where(USER_BALANCES.BALANCE_ID.eq(balanceId))
            .fetchOne()!!
        assertThat(updatedBalance.currentBalance).isEqualTo(160)
        assertThat(updatedBalance.totalSpent).isEqualTo(40)
        assertThat(mainDsl.fetchCount(BALANCE_TRANSACTIONS)).isEqualTo(1)
        assertThat(journalDsl.fetchCount(ledgerEvents)).isEqualTo(1)
    }

    @Test
    fun `journal failure rolls back main db changes`() {
        val userId = insertUser()
        val balanceId = insertBalance(userId, currentBalance = 150)
        val rewardId = insertReward(pointsCost = 60, quantity = 2)

        installLedgerInsertFailureTrigger()

        assertFailsWith<Exception> {
            rewardService.redeemReward(userId, rewardId)
        }

        val balance = mainDsl.selectFrom(USER_BALANCES)
            .where(USER_BALANCES.BALANCE_ID.eq(balanceId))
            .fetchOne()!!
        val reward = mainDsl.selectFrom(REWARDS)
            .where(REWARDS.REWARD_ID.eq(rewardId))
            .fetchOne()!!

        assertThat(balance.currentBalance).isEqualTo(150)
        assertThat(balance.totalSpent).isEqualTo(0)
        assertThat(reward.quantity).isEqualTo(2)
        assertThat(mainDsl.fetchCount(USER_REWARDS)).isZero()
        assertThat(mainDsl.fetchCount(BALANCE_TRANSACTIONS)).isZero()
        assertThat(journalDsl.fetchCount(ledgerEvents)).isZero()
    }

    @Test
    fun `ledger events table rejects update and delete`() {
        val operationId = UUID.randomUUID()
        journalDsl.insertInto(ledgerEvents)
            .set(field(name("operation_id"), UUID::class.java), operationId)
            .set(field(name("event_type"), String::class.java), "DONATION")
            .set(field(name("user_id"), UUID::class.java), UUID.randomUUID())
            .set(field(name("balance_id"), UUID::class.java), UUID.randomUUID())
            .set(field(name("amount"), Int::class.java), -10)
            .set(field(name("balance_before"), Int::class.java), 100)
            .set(field(name("balance_after"), Int::class.java), 90)
            .set(field(name("description"), String::class.java), "test")
            .execute()

        assertFailsWith<Exception> {
            journalDsl.execute("UPDATE ledger_events SET description = 'mutated' WHERE operation_id = '$operationId'")
        }
        assertFailsWith<Exception> {
            journalDsl.execute("DELETE FROM ledger_events WHERE operation_id = '$operationId'")
        }
    }

    @Test
    fun `ledger events repository enforces unique operation id`() {
        val operationId = UUID.randomUUID()
        journalDsl.insertInto(ledgerEvents)
            .set(field(name("operation_id"), UUID::class.java), operationId)
            .set(field(name("event_type"), String::class.java), "DONATION")
            .set(field(name("user_id"), UUID::class.java), UUID.randomUUID())
            .set(field(name("balance_id"), UUID::class.java), UUID.randomUUID())
            .set(field(name("amount"), Int::class.java), -10)
            .set(field(name("balance_before"), Int::class.java), 100)
            .set(field(name("balance_after"), Int::class.java), 90)
            .set(field(name("description"), String::class.java), "first")
            .execute()

        assertThat(ledgerEventService.countByOperationId(operationId)).isEqualTo(1)
        assertFailsWith<Exception> {
            journalDsl.insertInto(ledgerEvents)
                .set(field(name("operation_id"), UUID::class.java), operationId)
                .set(field(name("event_type"), String::class.java), "DONATION")
                .set(field(name("user_id"), UUID::class.java), UUID.randomUUID())
                .set(field(name("balance_id"), UUID::class.java), UUID.randomUUID())
                .set(field(name("amount"), Int::class.java), -10)
                .set(field(name("balance_before"), Int::class.java), 100)
                .set(field(name("balance_after"), Int::class.java), 90)
                .set(field(name("description"), String::class.java), "duplicate")
                .execute()
        }
    }

    private fun installLedgerInsertFailureTrigger() {
        journalDsl.execute(
            """
            CREATE OR REPLACE FUNCTION fail_ledger_insert()
            RETURNS TRIGGER
            LANGUAGE plpgsql
            AS $$
            BEGIN
                RAISE EXCEPTION 'forced ledger insert failure';
            END;
            $$;
            """.trimIndent(),
        )
        journalDsl.execute(
            """
            CREATE TRIGGER trg_fail_ledger_insert
            BEFORE INSERT ON ledger_events
            FOR EACH ROW
            EXECUTE FUNCTION fail_ledger_insert();
            """.trimIndent(),
        )
    }

    private fun insertUser(): UUID =
        mainDsl.insertInto(USERS)
            .set(USERS.USERNAME, "user-${UUID.randomUUID()}")
            .set(USERS.PASSWORD_HASH, "hashed")
            .set(USERS.ROLE, "USER")
            .set(USERS.EMAIL, "user-${UUID.randomUUID()}@test.local")
            .returningResult(USERS.ID)
            .fetchOne(USERS.ID)!!

    private fun insertBalance(userId: UUID, currentBalance: Int): UUID =
        mainDsl.insertInto(USER_BALANCES)
            .set(USER_BALANCES.USER_ID, userId)
            .set(USER_BALANCES.CURRENT_BALANCE, currentBalance)
            .set(USER_BALANCES.TOTAL_EARNED, currentBalance)
            .set(USER_BALANCES.TOTAL_SPENT, 0)
            .set(USER_BALANCES.VERSION, 1)
            .returningResult(USER_BALANCES.BALANCE_ID)
            .fetchOne(USER_BALANCES.BALANCE_ID)!!

    private fun insertReward(pointsCost: Int, quantity: Int): UUID =
        mainDsl.insertInto(REWARDS)
            .set(REWARDS.NAME, "reward-${UUID.randomUUID()}")
            .set(REWARDS.DESCRIPTION, "reward")
            .set(REWARDS.POINTS_COST, pointsCost)
            .set(REWARDS.IS_ACTIVE, true)
            .set(REWARDS.QUANTITY, quantity)
            .returningResult(REWARDS.REWARD_ID)
            .fetchOne(REWARDS.REWARD_ID)!!

    private fun testUser(userId: UUID) =
        JwtUserDetails(
            id = userId,
            _username = "test-user",
            email = Email("test-user@example.com"),
            role = "USER",
            token = "token",
            edxSession = null,
        )
}
