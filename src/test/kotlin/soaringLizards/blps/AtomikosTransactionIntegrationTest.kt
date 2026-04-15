package soaringLizards.blps

import org.assertj.core.api.Assertions.assertThat
import org.jooq.DSLContext
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.transaction.annotation.Transactional
import soaringLizards.blps.dto.balanceTransactions.BalanceTransactionDto
import soaringLizards.blps.dto.user.Email
import soaringLizards.blps.dto.user.RegisterDto
import soaringLizards.blps.dto.user.UserDto
import soaringLizards.blps.dto.userBalances.UserBalanceDto
import soaringLizards.blps.exception.BusinessLogicException
import soaringLizards.blps.integration.IntegrationPostgres
import soaringLizards.blps.integration.TestcontainersCleanupExtension
import soaringLizards.blps.jooq.generated.tables.references.BALANCE_TRANSACTIONS
import soaringLizards.blps.repository.BalanceTransactionRepository
import soaringLizards.blps.repository.UserBalanceRepository
import soaringLizards.blps.repository.UserRepository
import soaringLizards.blps.service.UserService
import java.time.OffsetDateTime
import java.util.UUID

@SpringBootTest
@Import(AtomikosRollbackProbeConfiguration::class)
@ExtendWith(TestcontainersCleanupExtension::class)
class AtomikosTransactionIntegrationTest {

    @Autowired
    private lateinit var rollbackProbe: TransactionRollbackProbe

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var userBalanceRepository: UserBalanceRepository

    @Autowired
    private lateinit var userService: UserService

    @Autowired
    private lateinit var dsl: DSLContext

    companion object {
        @JvmStatic
        @DynamicPropertySource
        fun registerProperties(registry: DynamicPropertyRegistry) {
            IntegrationPostgres.registerProperties(registry)
        }
    }

    @Test
    fun `register transaction should rollback inserted user on exception`() {
        val username = "rollback-user-${UUID.randomUUID()}"

        assertThrows<BusinessLogicException> {
            rollbackProbe.registerThenFail(username)
        }

        assertThat(userRepository.findByUsername(username)).isNull()
    }

    @Test
    fun `donation transaction should rollback balance update and transaction record on exception`() {
        val username = "donation-user-${UUID.randomUUID()}"
        userService.registerUser(
            RegisterDto(
                username = username,
                password = "password",
                email = Email("donation-${UUID.randomUUID()}@example.com"),
            ),
            "USER",
        )

        val user = userRepository.findByUsername(username)!!
        val balance = userBalanceRepository.findByUserId(user.id!!)!!
        val initialBalance = balance.currentBalance!!
        val marker = "forced-rollback-${UUID.randomUUID()}"

        assertThrows<BusinessLogicException> {
            rollbackProbe.donateThenFail(balance.balanceId!!, marker, 150)
        }

        val balanceAfterRollback = userBalanceRepository.findById(balance.balanceId!!)!!
        val transactionCount = dsl.fetchCount(
            BALANCE_TRANSACTIONS,
            BALANCE_TRANSACTIONS.DESCRIPTION.eq(marker),
        )

        assertThat(balanceAfterRollback.currentBalance).isEqualTo(initialBalance)
        assertThat(balanceAfterRollback.totalSpent).isZero()
        assertThat(balanceAfterRollback.version).isEqualTo(1)
        assertThat(transactionCount).isZero()
    }
}

@TestConfiguration
class AtomikosRollbackProbeConfiguration {
    @Bean
    fun transactionRollbackProbe(
        userRepository: UserRepository,
        userBalanceRepository: UserBalanceRepository,
        balanceTransactionRepository: BalanceTransactionRepository,
        passwordEncoder: PasswordEncoder,
    ) = TransactionRollbackProbe(
        userRepository = userRepository,
        userBalanceRepository = userBalanceRepository,
        balanceTransactionRepository = balanceTransactionRepository,
        passwordEncoder = passwordEncoder,
    )
}

open class TransactionRollbackProbe(
    private val userRepository: UserRepository,
    private val userBalanceRepository: UserBalanceRepository,
    private val balanceTransactionRepository: BalanceTransactionRepository,
    private val passwordEncoder: PasswordEncoder,
) {

    @Transactional
    open fun registerThenFail(username: String) {
        val user = userRepository.insert(
            UserDto(
                username = username,
                password = passwordEncoder.encode("password")!!,
                email = Email("$username@example.com"),
                role = "USER",
            ),
        ) ?: error("Expected inserted user")

        userBalanceRepository.insert(UserBalanceDto.createDefault(user.id!!))
        throw BusinessLogicException("Force rollback after registration writes")
    }

    @Transactional
    open fun donateThenFail(balanceId: UUID, marker: String, amount: Int) {
        val balance = userBalanceRepository.findById(balanceId) ?: error("Balance not found")
        val currentBalance = balance.currentBalance!!

        balanceTransactionRepository.insert(
            BalanceTransactionDto(
                balanceId = balanceId,
                transactionType = "DONATION",
                amount = -amount,
                balanceBefore = currentBalance,
                balanceAfter = currentBalance - amount,
                userAchievementId = null,
                achievementTypeId = null,
                description = marker,
                createdAt = OffsetDateTime.now(),
            ),
        )
        userBalanceRepository.update(
            balanceId,
            UserBalanceDto(
                userId = balance.userId!!,
                currentBalance = currentBalance - amount,
                totalEarned = balance.totalEarned!!,
                totalSpent = balance.totalSpent!! + amount,
                version = balance.version!! + 1,
            ),
        )
        throw BusinessLogicException("Force rollback after donation writes")
    }
}
