package soaringlizards.datalakesync.spark

import soaringlizards.datalakesync.config.SparkSqlConfig
import java.util.concurrent.TimeUnit

class SparkSqlExecutor(private val config: SparkSqlConfig) {
    class Builder(private val base: SparkSqlConfig) {
        companion object {
            private const val SPARK_TIMEOUT_SECONDS = 90L
        }

        fun execute(sql: String): SparkSqlResult = run(sql)

        private fun run(sql: String): SparkSqlResult {
            val execCmd = listOf(
                "docker", "exec",
                base.container,
                "spark-sql",
            ) + listOf("-e", sql)

            val pb = ProcessBuilder(execCmd)
            pb.redirectErrorStream(true)
            val process = pb.start()
            val reader = process.inputStream.bufferedReader()
            val exited = process.waitFor(SPARK_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            val stdout = reader.readText()
            reader.close()
            if (!exited) {
                process.destroyForcibly()
                return SparkSqlResult("$stdout\n[TIMEOUT]", "", -1)
            }
            return SparkSqlResult(stdout, "", process.exitValue())
        }
    }

    fun builder() = Builder(config)

    companion object {
        @JvmStatic
        fun builder(config: SparkSqlConfig) = Builder(config)
    }
}
