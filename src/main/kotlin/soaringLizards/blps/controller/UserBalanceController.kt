package soaringLizards.blps.controller

import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.userdetails.User
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import soaringLizards.blps.dto.balanceTransactions.DonateRequest
import soaringLizards.blps.service.UserBalanceService
import soaringLizards.blps.service.UserService

@RestController("/api/user-balance")
class UserBalanceController(
    private val userBalanceService: UserBalanceService,
    private val userService: UserService,
) {

    @GetMapping("/")
    fun getCurrentUserBalance(@AuthenticationPrincipal currentUser: User?) =
        userBalanceService.getCurrentUserBalance(currentUser)

    @PostMapping("/donate")
    fun spendUserBalance(@AuthenticationPrincipal currentUser: User?, @RequestBody request: DonateRequest) =
        userBalanceService.donatePoints(currentUser, request)
}
