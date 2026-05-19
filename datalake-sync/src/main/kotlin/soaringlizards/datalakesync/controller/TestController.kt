package soaringlizards.datalakesync.controller

import org.springframework.context.annotation.Profile
import org.springframework.web.bind.annotation.*
import soaringlizards.datalakesync.spark.DevSparkSqlService

@RestController
@RequestMapping("/test")
@Profile("dev")
class TestController(private val devSparkSqlService: DevSparkSqlService) {

    @PostMapping("/write")
    fun write(@RequestBody body: TestWriteRequest) =
        devSparkSqlService.insertRow(body.id, body.comment)

    @GetMapping("/read")
    fun read() =
        devSparkSqlService.selectAll()

    data class TestWriteRequest(val id: Int, val comment: String)
}
