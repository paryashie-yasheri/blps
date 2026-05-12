package soaringlizards.datalakesync

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication
import soaringlizards.datalakesync.config.SparkSqlConfig

@SpringBootApplication
@EnableConfigurationProperties(SparkSqlConfig::class)
class DatalakeSyncApplication

fun main(args: Array<String>) {
    runApplication<DatalakeSyncApplication>(*args)
}
