package soaringLizards.blps.controller

import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import soaringLizards.blps.service.TransactionService


@RestController
@RequestMapping("/api/transactions")
class BalanceTransactionsController(
    val transactionService: TransactionService,
) {

//    @GetMapping("by-user-id/{id}")
//    fun getAll(@RequestParam id: UUID?, @) {
//        transactionService.
//    }
}
