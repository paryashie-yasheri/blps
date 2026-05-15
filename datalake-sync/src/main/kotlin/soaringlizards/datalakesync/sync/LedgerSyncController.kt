package soaringlizards.datalakesync.sync

import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/ledger")
@Profile("dev", "prod")
class LedgerSyncController(private val syncService: LedgerSyncService) {
    private val logger = LoggerFactory.getLogger(LedgerSyncController::class.java)

    @PostMapping("/sync")
    fun sync() = try {
        val result = syncService.sync()
        mapOf(
            "status" to "ok",
            "fetched" to result.fetched,
            "inserted" to result.inserted,
            "cursor" to result.cursor?.toString(),
        )
    } catch (e: Exception) {
        logger.error("Manual sync failed", e)
        mapOf("status" to "error", "message" to (e.message ?: "unknown"))
    }
}
