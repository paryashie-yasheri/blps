package soaringLizards.blps.controller

import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import soaringLizards.blps.config.userDetails.JwtUserDetails
import soaringLizards.blps.dto.balanceTransactions.DonateRequest
import soaringLizards.blps.service.UserBalanceService

@RestController
@RequestMapping(("/api/user-balance"))
class UserBalanceController(
    private val userBalanceService: UserBalanceService,
) {

    @PreAuthorize("hasRole('OPENEDX_USER')")
    @GetMapping("/")
    fun getCurrentUserBalance(@AuthenticationPrincipal currentUser: JwtUserDetails) =
        userBalanceService.getCurrentUserBalance(currentUser.id)

    @PreAuthorize("hasRole('OPENEDX_USER')")
    @PostMapping("/donate")
    fun spendUserBalance(@AuthenticationPrincipal currentUser: JwtUserDetails, @RequestBody request: DonateRequest) =
        userBalanceService.donatePoints(currentUser, request)
}
