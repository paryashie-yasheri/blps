package soaringLizards.blps.controller

import org.springframework.security.access.AccessDeniedException
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import soaringLizards.blps.config.userDetails.JwtUserDetails
import soaringLizards.blps.dto.user.LoginRequestDto
import soaringLizards.blps.dto.user.RegisterDto
import soaringLizards.blps.dto.user.toUserResponse
import soaringLizards.blps.service.UserService


@RestController
@RequestMapping("/api/auth")
class AuthController(
    private val userService: UserService,
) {
    @GetMapping("/me")
    fun getMe(@AuthenticationPrincipal principal: JwtUserDetails?) =
        principal?.toUserResponse() ?: throw AccessDeniedException("Not logged in")

    @PostMapping("/register-admin")
    @PreAuthorize("hasRole('ADMIN')")
    fun registerAdmin(@RequestBody request: RegisterDto) =
        userService.registerUser(request, "ADMIN").toUserResponse()

    @PostMapping("/register-staff")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    fun registerStaff(@RequestBody request: RegisterDto) =
        userService.registerUser(request, "STAFF").toUserResponse()

    @PostMapping("/staff/login")
    fun loginStaff(@RequestBody request: LoginRequestDto) =
        userService.loginStaff(request)

    @PostMapping("/openedx_user/login")
    fun loginOpenedxUser(@RequestBody request: LoginRequestDto) =
        userService.loginOpenedxUser(request)
}
