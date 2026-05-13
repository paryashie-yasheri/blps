package soaringlizards.datalakesync.sync

import org.quartz.DisallowConcurrentExecution
import org.quartz.Job
import org.quartz.JobExecutionContext
import org.quartz.JobExecutionException
import org.slf4j.LoggerFactory

@DisallowConcurrentExecution
class LedgerSyncJob : Job {
    private val logger = LoggerFactory.getLogger(LedgerSyncJob::class.java)

    override fun execute(context: JobExecutionContext) {
        val service = context.scheduler.context["ledgerSyncService"] as? LedgerSyncService
            ?: throw JobExecutionException("LedgerSyncService not found in scheduler context")
        try {
            val result = service.sync()
            logger.info("Ledger sync completed: fetched=${result.fetched}, inserted=${result.inserted}, cursor=${result.cursor}")
        } catch (e: Exception) {
            logger.error("Ledger sync failed", e)
            throw JobExecutionException(e)
        }
    }
}
