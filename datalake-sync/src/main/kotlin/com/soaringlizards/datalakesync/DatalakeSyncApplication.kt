package com.soaringlizards.datalakesync

import com.soaringlizards.datalakesync.config.SparkSqlConfig
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication

@SpringBootApplication
@EnableConfigurationProperties(SparkSqlConfig::class)
class DatalakeSyncApplication

fun main(args: Array<String>) {
    runApplication<DatalakeSyncApplication>(*args)
}
