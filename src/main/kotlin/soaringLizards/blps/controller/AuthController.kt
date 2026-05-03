package soaringLizards.blps.controller

import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import org.springframework.web.client.HttpClientErrorException
import soaringLizards.blps.config.userDetails.JwtUserDetails
import soaringLizards.blps.dto.user.LoginRequestDto
import soaringLizards.blps.dto.user.RegisterDto
import soaringLizards.blps.dto.user.UserResponse
import soaringLizards.blps.dto.user.toUserResponse
import soaringLizards.blps.service.KeycloakService


@RestController
@RequestMapping("/api/auth")
class AuthController(
    private val keycloakService: KeycloakService,
) {
    @GetMapping("/me")
    fun getMe(@AuthenticationPrincipal authentication: JwtUserDetails?): UserResponse {

        return authentication?.toUserResponse() ?: throw HttpClientErrorException(
            HttpStatus.UNAUTHORIZED,
            "Unauthorized",
        )
    }

    @PostMapping("/register-admin")
    @PreAuthorize("hasRole('ADMIN')")
    fun registerAdmin(@RequestBody request: RegisterDto) =
        keycloakService.registerUser(request, "ADMIN")

    @PostMapping("/register-staff")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    fun registerStaff(@RequestBody request: RegisterDto) =
        keycloakService.registerUser(request, "STAFF")

    @PostMapping("/staff/login")
    fun loginStaff(@RequestBody request: LoginRequestDto) =
        keycloakService.loginStaff(request)
//
//    @PostMapping("/openedx_user/login")
//    fun loginOpenedxUser(@RequestBody request: LoginRequestDto) =
//        keycloakService.loginOpenedxUser(request)
}
