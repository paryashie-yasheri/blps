package soaringlizards.datalakesync.spark

data class SparkSqlResult(
    val stdout: String,
    val stderr: String,
    val exitCode: Int,
) {
    val isSuccess: Boolean get() = exitCode == 0
}
