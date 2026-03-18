package soaringLizards.blps.controller;

import org.springframework.security.core.userdetails.User as SpringUser
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.security.access.AccessDeniedException
import soaringLizards.blps.dto.user.RegisterDto
import soaringLizards.blps.dto.user.UserResponse
import soaringLizards.blps.dto.user.toUserResponse
import soaringLizards.blps.exception.UserAlreadyExistAuthenticationException
import soaringLizards.blps.exception.UserNotFoundException
import soaringLizards.blps.service.UserService

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
