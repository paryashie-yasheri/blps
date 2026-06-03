package soaringLizards.blps.camunda.config

import org.camunda.bpm.engine.ProcessEngine
import org.camunda.bpm.engine.impl.history.HistoryLevel
import org.camunda.bpm.engine.spring.SpringProcessEngineConfiguration
import org.camunda.bpm.engine.spring.SpringProcessEngineServicesConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.core.io.FileSystemResource
import org.springframework.transaction.PlatformTransactionManager
import javax.sql.DataSource

@Configuration
@Import(SpringProcessEngineServicesConfiguration::class)
class CamundaEngineConfig {

    @Bean
    fun processEngineConfiguration(
        dataSource: DataSource,
        transactionManager: PlatformTransactionManager,
    ): SpringProcessEngineConfiguration {
        return SpringProcessEngineConfiguration().apply {
            this.dataSource = dataSource
            this.transactionManager = transactionManager
            databaseSchemaUpdate = "true"
            historyLevel = HistoryLevel.HISTORY_LEVEL_FULL
            processEngineName = "blps-process-engine"
            isJobExecutorActivate = true
            deploymentResources = arrayOf(
                FileSystemResource("src/main/resources/processes"),
            )
            deploymentName = "blps-deployment"
            setDeployChangedOnly(true)
        }
    }

    @Bean
    fun processEngine(config: SpringProcessEngineConfiguration): ProcessEngine {
        return config.buildProcessEngine()
    }
}
