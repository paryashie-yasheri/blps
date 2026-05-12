package com.soaringlizards.datalakesync.spark

import com.soaringlizards.datalakesync.config.SparkSqlConfig
import jakarta.annotation.PostConstruct
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

@Service
@Profile("!test")
class SparkSqlService(private val config: SparkSqlConfig) {

    private val exec = SparkSqlExecutor.builder(config)

    @PostConstruct
    fun initialize() {
        try {
            createNamespaceViaRest("mydb")
            createTableViaRest("mydb", "testtable")
            println("Iceberg table mydb.testtable ready")
        } catch (e: Exception) {
            println("Init failed: ${e.message}")
        }
    }

    private fun createNamespaceViaRest(ns: String) {
        val client = HttpClient.newHttpClient()
        val body = """{"namespace": ["$ns"]}"""
        val request = HttpRequest.newBuilder()
            .uri(URI("http://localhost:8181/v1/namespaces"))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build()
        try {
            val response = client.send(request, HttpResponse.BodyHandlers.ofString())
            if (response.statusCode() == 200 || response.statusCode() == 201) {
                println("Namespace $ns ready")
            } else if (response.body().contains("already exists")) {
                println("Namespace $ns already exists")
            } else {
                println("Create namespace response: ${response.statusCode()} ${response.body()}")
            }
        } catch (e: Exception) {
            println("Create namespace failed: ${e.message}")
        }
    }

    private fun createTableViaRest(ns: String, table: String) {
        val client = HttpClient.newHttpClient()
        val schema = """
            {
              "schema": {
                "fields": [
                  {"name": "id", "type": "int", "required": true},
                  {"name": "ts", "type": "timestamp", "required": false},
                  {"name": "comment", "type": "string", "required": false}
                ]
              }
            }
        """.trimIndent()
        val body = """{"name": "$table", "schema": $schema}"""
        val request = HttpRequest.newBuilder()
            .uri(URI("http://localhost:8181/v1/namespaces/$ns/tables"))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build()
        try {
            val response = client.send(request, HttpResponse.BodyHandlers.ofString())
            if (response.statusCode() == 200 || response.statusCode() == 201) {
                println("Table $ns.$table ready")
            } else if (response.body().contains("already exists")) {
                println("Table $ns.$table already exists")
            } else {
                println("Create table response: ${response.statusCode()} ${response.body()}")
            }
        } catch (e: Exception) {
            println("Create table failed: ${e.message}")
        }
    }

    fun insertRow(id: Int, comment: String): SparkSqlResult {
        val safeComment = comment
            .replace("\\", "\\\\")
            .replace("'", "''")
        return exec.execute(
            "INSERT INTO mydb.testtable VALUES ($id, current_timestamp(), '$safeComment')",
        )
    }

    fun selectAll(): SparkSqlResult {
        return exec.execute("SELECT * FROM mydb.testtable")
    }
}
