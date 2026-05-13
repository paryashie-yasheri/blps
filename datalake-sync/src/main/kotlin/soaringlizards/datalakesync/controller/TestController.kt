package soaringlizards.datalakesync.controller

import org.springframework.context.annotation.Profile
import org.springframework.web.bind.annotation.*
import soaringlizards.datalakesync.spark.DevSparkSqlService
import soaringlizards.datalakesync.spark.SparkSqlResult

@RestController
@RequestMapping("/test")
@Profile("dev")
class TestController(private val devSparkSqlService: DevSparkSqlService) {

    @PostMapping("/write")
    fun write(@RequestBody body: WriteRequest): SparkSqlResult {
        return devSparkSqlService.insertRow(body.id, body.comment)
    }

    @GetMapping("/read")
    fun read(): SparkSqlResult {
        return devSparkSqlService.selectAll()
    }

    data class WriteRequest(val id: Int, val comment: String)
}
