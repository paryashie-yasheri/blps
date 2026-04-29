package soaringLizards.blps

import com.atomikos.jdbc.AtomikosDataSourceBean
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.assertj.core.api.Assertions.assertThat
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.transaction.jta.JtaTransactionManager
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import soaringLizards.blps.integration.IntegrationPostgres
import soaringLizards.blps.integration.TestcontainersCleanupExtension
import javax.sql.DataSource

@SpringBootTest
@ExtendWith(TestcontainersCleanupExtension::class)
class ApplicationTests {

    @Autowired
    private lateinit var dataSource: DataSource

    @Autowired
    @Qualifier("journalDataSource")
    private lateinit var journalDataSource: DataSource

    @Autowired
    private lateinit var transactionManager: JtaTransactionManager


    companion object {
        @JvmStatic
        @DynamicPropertySource
        fun registerProperties(registry: DynamicPropertyRegistry) {
            IntegrationPostgres.registerProperties(registry)
        }
    }

    @Test
    fun contextLoads() {
        assertThat(dataSource).isInstanceOf(AtomikosDataSourceBean::class.java)
        assertThat(journalDataSource).isInstanceOf(AtomikosDataSourceBean::class.java)
        assertThat(transactionManager).isInstanceOf(JtaTransactionManager::class.java)
    }
}
