package soaringlizards.datalakesync.sync

import org.quartz.CronScheduleBuilder.cronSchedule
import org.quartz.JobBuilder.newJob
import org.quartz.TriggerBuilder.newTrigger
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.quartz.SchedulerFactoryBean
import soaringlizards.datalakesync.config.LedgerSyncConfig

@Configuration
@ConditionalOnProperty(name = ["datalake.quartz.enabled"], havingValue = "true", matchIfMissing = true)
class LedgerSyncSchedulerConfig(
    private val applicationContext: ApplicationContext,
    private val syncConfig: LedgerSyncConfig,
) {

    @Bean
    fun ledgerSyncScheduler(factory: SchedulerFactoryBean): org.quartz.Scheduler {
        val service = applicationContext.getBean(LedgerSyncService::class.java)

        val job = newJob(LedgerSyncJob::class.java)
            .withIdentity("ledgerSyncJob", "datalake")
            .storeDurably()
            .build()
        job.jobDataMap["ledgerSyncService"] = service

        val trigger = newTrigger()
            .withIdentity("ledgerSyncTrigger", "datalake")
            .withSchedule(cronSchedule(syncConfig.sync.cron))
            .forJob(job)
            .build()

        val scheduler = factory.`object`!!
        scheduler.context["ledgerSyncService"] = service
        scheduler.scheduleJob(job, trigger)
        scheduler.start()
        return scheduler
    }
}
