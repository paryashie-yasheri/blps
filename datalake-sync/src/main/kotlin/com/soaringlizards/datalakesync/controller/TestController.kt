package com.soaringlizards.datalakesync.controller

import com.soaringlizards.datalakesync.spark.SparkSqlResult
import com.soaringlizards.datalakesync.spark.SparkSqlService
import org.springframework.context.annotation.Profile
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/test")
@Profile("dev")
class TestController(private val sparkSqlService: SparkSqlService) {

    @PostMapping("/write")
    fun write(@RequestBody body: WriteRequest): SparkSqlResult {
        return sparkSqlService.insertRow(body.id, body.comment)
    }

    @GetMapping("/read")
    fun read(): SparkSqlResult {
        return sparkSqlService.selectAll()
    }

    data class WriteRequest(val id: Int, val comment: String)
}