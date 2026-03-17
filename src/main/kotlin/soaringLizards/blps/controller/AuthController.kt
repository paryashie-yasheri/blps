package soaringLizards.blps.controller;

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import soaringLizards.blps.dto.user.requests.RegisterDto
import soaringLizards.blps.exception.UserAlreadyExistAuthenticationException
import soaringLizards.blps.service.UserService

@RestController
@RequestMapping("/api/auth")
class AuthController(
    private val userService: UserService,
) {
    @PostMapping("/register")
    fun register(@RequestBody request: RegisterDto): ResponseEntity<*> {
        try {
            userService.registerUser(request);
            return ResponseEntity.ok("User registered")
        } catch (e: UserAlreadyExistAuthenticationException) {
            return ResponseEntity.badRequest().body(e.message);
        }
    }
}