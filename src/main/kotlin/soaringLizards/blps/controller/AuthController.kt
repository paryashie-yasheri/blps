package soaringLizards.blps.controller;

import jakarta.annotation.PostConstruct
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import soaringLizards.blps.dto.user.RegisterDto
import soaringLizards.blps.repository.UserRepository

@RestController
@RequestMapping("/api/auth")
class AuthController(
    private val userRepository: UserRepository,
) {
    companion object {
        private val logger = LoggerFactory.getLogger(AuthController::class.java)
    }

    @PostConstruct
    fun init() {
        logger.info("🚀 AuthController started - mapping /api/auth/**")
    }

    @PostMapping("/register")
    fun register(@RequestBody request: RegisterDto): ResponseEntity<*> {
        if (userRepository.findByUsername(request.username) != null) {
            return ResponseEntity.badRequest().body("User exists")
        }

        userRepository.insert(request)
        return ResponseEntity.ok("User registered")
    }
}