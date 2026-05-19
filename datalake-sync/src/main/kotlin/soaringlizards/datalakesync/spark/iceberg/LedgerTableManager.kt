package soaringlizards.datalakesync.spark.iceberg

import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClient

@Service
@Profile("!test")
class LedgerTableManager(private val restClient: RestClient) {

    private val ns = "datalake"
    private val tbl = "ledger_events"

    fun createNamespace() {
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

    fun createTable(): Boolean {
        val fields = listOf(
            mapOf("name" to "event_id", "id" to 1, "type" to "string", "required" to false),
            mapOf("name" to "operation_id", "id" to 2, "type" to "string", "required" to true),
            mapOf("name" to "event_type", "id" to 3, "type" to "string", "required" to true),
            mapOf("name" to "occurred_at", "id" to 4, "type" to "timestamp", "required" to true),
            mapOf("name" to "user_id", "id" to 5, "type" to "string", "required" to true),
            mapOf("name" to "balance_id", "id" to 6, "type" to "string", "required" to true),
            mapOf("name" to "amount", "id" to 7, "type" to "int", "required" to true),
            mapOf("name" to "balance_before", "id" to 8, "type" to "int", "required" to true),
            mapOf("name" to "balance_after", "id" to 9, "type" to "int", "required" to true),
            mapOf("name" to "reward_id", "id" to 10, "type" to "string", "required" to false),
            mapOf("name" to "description", "id" to 11, "type" to "string", "required" to false),
        )

        val schema = mapOf("type" to "struct", "fields" to fields)
        val body = mapOf(
            "name" to tbl,
            "schema" to schema,
            "properties" to mapOf(
                "format-version" to "2",
                "write.distribution-mode" to "hash",
            ),
        )

        return try {
            val response = restClient.post()
                .uri("/v1/namespaces/$ns/tables")
                .body(body)
                .retrieve()
                .toEntity(String::class.java)

            when {
                response.statusCode.is2xxSuccessful -> {
                    println("Table $ns.$tbl ready")
                    true
                }

                response.body?.contains("already exists") == true -> {
                    println("Table $ns.$tbl already exists")
                    true
                }

                else -> {
                    println("Create table response: ${response.statusCode} ${response.body}")
                    false
                }
            }
        } catch (e: Exception) {
            println("Create table failed: ${e.message}")
            false
        }
    }
}
