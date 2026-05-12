package soaringlizards.datalakesync.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "datalake.ledger")
data class LedgerSyncConfig(
    val db: Db = Db(),
    val sync: Sync = Sync(),
) {
    data class Db(
        val url: String = "jdbc:postgresql://localhost:5433/studs_journal",
        val username: String = "postgres",
        val password: String = "postgres",
    )

    data class Sync(
        val batchSize: Int = 1000,
        val cron: String = "0 0 3 * * ?",
    )
}
