package soaringLizards.blps.controller;

import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import soaringLizards.blps.dto.user.RegisterDto
import soaringLizards.blps.service.UserService
import org.springframework.security.core.userdetails.User as SpringUser

@RestController
@RequestMapping("/api/auth")
class AuthController(
    private val userService: UserService,
) {
    @GetMapping("/me")
    fun getMe(@AuthenticationPrincipal currentUser: SpringUser?) =
        userService.getUserFromAuthenticationPrincipal(currentUser)

    @PostMapping("/register")
    fun registerUser(@RequestBody request: RegisterDto) =
        userService.registerUser(request, "USER")

    @PostMapping("/register-admin")
    @PreAuthorize("hasRole('ADMIN')")
    fun registerAdmin(@RequestBody request: RegisterDto) =
        userService.registerUser(request, "ADMIN")

    @PostMapping("/register-staff")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    fun registerStaff(@RequestBody request: RegisterDto) =
        userService.registerUser(request, "STAFF")
}
