package soaringlizards.datalakesync.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "datalake.spark")
data class SparkSqlConfig(
    val container: String = "spark-iceberg",
    val catalog: Catalog = Catalog(),
    val warehouse: Warehouse = Warehouse(),
    val s3: S3 = S3(),
    val network: String? = null,
) {
    data class Catalog(
        val name: String = "demo",
        val uri: String = "http://rest:8181",
    )

    data class Warehouse(
        val uri: String = "s3://warehouse/",
    )

    data class S3(
        val endpoint: String = "http://minio:9000",
        val accessKey: String = "admin",
        val secretKey: String = "password",
    )
}
