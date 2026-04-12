package soaringLizards.blps.controller

import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.userdetails.User
import org.springframework.web.bind.annotation.*
import soaringLizards.blps.dto.balanceTransactions.DonateRequest
import soaringLizards.blps.service.UserBalanceService

@RestController
@RequestMapping(("/api/user-balance"))
class UserBalanceController(
    private val userBalanceService: UserBalanceService,
) {

    @GetMapping("/")
    fun getCurrentUserBalance(@AuthenticationPrincipal currentUser: User?) =
        userBalanceService.getCurrentUserBalance(currentUser)

    @PostMapping("/donate")
    fun spendUserBalance(@AuthenticationPrincipal currentUser: User?, @RequestBody request: DonateRequest) =
        userBalanceService.donatePoints(currentUser, request)
}
