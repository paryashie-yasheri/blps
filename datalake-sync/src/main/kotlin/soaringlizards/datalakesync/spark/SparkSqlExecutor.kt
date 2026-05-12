package soaringlizards.datalakesync.spark

import soaringlizards.datalakesync.config.SparkSqlConfig
import java.util.concurrent.TimeUnit

class SparkSqlExecutor(private val config: SparkSqlConfig) {

    class Builder(private val base: SparkSqlConfig) {
        private var catalogUri: String = base.catalog.uri
        private var s3Endpoint: String = base.s3.endpoint
        private var accessKey: String = base.s3.accessKey
        private var secretKey: String = base.s3.secretKey

        fun catalogUri(uri: String) = apply { catalogUri = uri }
        fun s3Endpoint(url: String) = apply { s3Endpoint = url }
        fun credentials(key: String, secret: String) = apply {
            accessKey = key; secretKey = secret
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

            val stdout = process.inputStream.bufferedReader().readText()
            try {
                val timedOut = !process.waitFor(90, TimeUnit.SECONDS)
                if (timedOut) {
                    process.destroyForcibly()
                    return SparkSqlResult("$stdout\n[TIMEOUT]", "", -1)
                }
            } catch (e: InterruptedException) {
                process.destroyForcibly()
                return SparkSqlResult("$stdout\n[INTERRUPTED]", "", -1)
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
