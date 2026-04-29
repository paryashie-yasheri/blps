package soaringLizards.blps.controller

import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*
import org.springframework.web.client.HttpClientErrorException
import soaringLizards.blps.dto.user.Email
import soaringLizards.blps.dto.user.LoginRequestDto
import soaringLizards.blps.dto.user.RegisterDto
import soaringLizards.blps.dto.user.UserResponse
import soaringLizards.blps.service.UserService


@RestController
@RequestMapping("/api/auth")
class AuthController(
    private val userService: UserService,
) {
    @GetMapping("/me")
    fun getMe(authentication: Authentication?): UserResponse {
        if (authentication == null || !authentication.isAuthenticated) {
            throw HttpClientErrorException(HttpStatus.UNAUTHORIZED, "Unauthorized")
        }

        val name = authentication.name

        val authorities = authentication.authorities.map { it.authority }
        val role = when {
            authorities.contains("ROLE_ADMIN") -> "ADMIN"
            authorities.contains("ROLE_STAFF") -> "STAFF"
            else -> "USER"
        }

        val user = userService.getUserByUsername(name)
        if (user != null) {
            return user
        }

        return UserResponse(
            username = name,
            email = Email("$name@blps.local"),
            role = role,
        )
    }

    @PostMapping("/register-admin")
    @PreAuthorize("hasRole('ADMIN')")
    fun registerAdmin(@RequestBody request: RegisterDto) =
        userService.registerUser(request, "ADMIN")

    @PostMapping("/register-staff")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    fun registerStaff(@RequestBody request: RegisterDto) =
        userService.registerUser(request, "STAFF")

    @PostMapping("/staff/login")
    fun loginStaff(@RequestBody request: LoginRequestDto) =
        userService.loginStaff(request)

    @PostMapping("/openedx_user/login")
    fun loginOpenedxUser(@RequestBody request: LoginRequestDto) =
        userService.loginOpenedxUser(request)
}