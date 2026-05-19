package soaringlizards.datalakesync.controller

import org.springframework.context.annotation.Profile
import org.springframework.web.bind.annotation.*
import soaringlizards.datalakesync.dto.WriteRequest
import soaringlizards.datalakesync.spark.iceberg.LedgerDataLakeService

@RestController
@RequestMapping("/ledger")
@Profile("dev", "prod")
class LedgerController(private val ledgerDataLakeService: LedgerDataLakeService) {

    @PostMapping("/write")
    fun write(@RequestBody request: WriteRequest) =
        ledgerDataLakeService.insert(request)

    @GetMapping("/read")
    fun read(@RequestParam(defaultValue = "100") limit: Int) =
        ledgerDataLakeService.read(limit)


    @GetMapping("/read/user")
    fun readByUser(
        @RequestParam userId: String,
        @RequestParam(defaultValue = "100") limit: Int,
    ) = ledgerDataLakeService.readByUser(userId, limit)


    @GetMapping("/count")
    fun count() =
        ledgerDataLakeService.count()

    @PostMapping("/seed")
    fun seed() =
        ledgerDataLakeService.seed()
}
