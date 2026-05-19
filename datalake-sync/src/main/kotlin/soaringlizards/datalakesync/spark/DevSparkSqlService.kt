package soaringlizards.datalakesync.spark

import jakarta.annotation.PostConstruct
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClient
import soaringlizards.datalakesync.config.SparkSqlConfig

@Service
@Profile("dev")
class DevSparkSqlService(
    private val config: SparkSqlConfig,
    private val restClient: RestClient,
) {
    private val exec = SparkSqlExecutor.builder(config)

    @PostConstruct
    fun initialize() {
        try {
            createNamespaceViaRest("mydb")
            if (config.createTestTable) {
                createTableViaRest("mydb", "testtable")
                println("Iceberg table mydb.testtable ready")
            }
        } catch (e: Exception) {
            println("Init failed: ${e.message}")
        }
    }

    private fun createNamespaceViaRest(ns: String) {
        try {
            val response = restClient.post()
                .uri("/v1/namespaces")
                .body(mapOf("namespace" to listOf(ns)))
                .retrieve()
                .toEntity(String::class.java)
            when {
                response.statusCode.is2xxSuccessful -> println("Namespace $ns ready")
                response.body?.contains("already exists") == true -> println("Namespace $ns already exists")
                else -> println("Create namespace response: ${response.statusCode} ${response.body}")
            }
        } catch (e: Exception) {
            println("Create namespace failed: ${e.message}")
        }
    }

    private fun createTableViaRest(ns: String, table: String) {
        val schema = mapOf(
            "type" to "struct",
            "fields" to listOf(
                mapOf("id" to mapOf("type" to "int", "optional" to false)),
                mapOf("ts" to mapOf("type" to "timestamp", "optional" to true)),
                mapOf("comment" to mapOf("type" to "string", "optional" to true)),
            ),
        )
        try {
            val response = restClient.post()
                .uri("/v1/namespaces/$ns/tables")
                .body(mapOf("name" to table, "schema" to schema))
                .retrieve()
                .toEntity(String::class.java)
            when {
                response.statusCode.is2xxSuccessful -> println("Table $ns.$table ready")
                response.body?.contains("already exists") == true -> println("Table $ns.$table already exists")
                else -> println("Create table response: ${response.statusCode} ${response.body}")
            }
        } catch (e: Exception) {
            println("Create table failed: ${e.message}")
        }
    }

    fun insertRow(id: Int, comment: String): SparkSqlResult {
        return query(
            "INSERT INTO mydb.testtable VALUES (?, ?, ?)",
            QueryParam.int(id),
            QueryParam.raw("current_timestamp()"),
            QueryParam.str(comment),
        ).executeWith(exec)
    }

    fun selectAll(): SparkSqlResult {
        return query("SELECT * FROM mydb.testtable").executeWith(exec)
    }
}
