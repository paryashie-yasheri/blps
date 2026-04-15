package soaringLizards.blps.controller

import org.slf4j.LoggerFactory
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import soaringLizards.blps.config.EdxAuthenticatedSession
import soaringLizards.blps.config.EdxSessionAuthManager
import soaringLizards.blps.config.JwtUserDetails
import soaringLizards.blps.dto.user.LoginRequestDto
import soaringLizards.blps.dto.user.LoginResponseDto
import soaringLizards.blps.dto.user.RegisterDto
import soaringLizards.blps.dto.user.UserResponse
import soaringLizards.blps.dto.user.toUserResponse
import soaringLizards.blps.exception.UserNotFoundException
import soaringLizards.blps.service.JwtTokenService
import soaringLizards.blps.service.UserService

private val logger = LoggerFactory.getLogger(AuthController::class.java)

@RestController
@RequestMapping("/api/auth")
class AuthController(
    private val userService: UserService,
    private val jwtTokenService: JwtTokenService,
    private val edxSessionAuthManager: EdxSessionAuthManager,
) {
    @GetMapping("/me")
    fun getMe(
        @AuthenticationPrincipal principal: JwtUserDetails?,
    ): UserResponse {
        val username = principal?.username
            ?: throw AccessDeniedException("Not logged in")
        
        return userService.getUserByUsername(username)
            ?: throw UserNotFoundException("User not found in database")
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
    fun staffLogin(@RequestBody request: LoginRequestDto): LoginResponseDto {
        val user = userService.authenticateStaff(request.username, request.password)
            ?: throw org.springframework.security.access.AccessDeniedException("Invalid credentials")
        
        val token = jwtTokenService.generateStaffToken(user.username, setOf(user.role))
        
        return LoginResponseDto(
            accessToken = token,
            tokenType = "Bearer",
            expiresIn = 3600,
            username = user.username,
            role = user.role
        )
    }

    @PostMapping("/openedx_user/login")
    fun openedxUserLogin(@RequestBody request: LoginRequestDto): LoginResponseDto {
        logger.info("Login attempt for user: ${request.username}")
        
        val lmsResult = try {
            edxSessionAuthManager.authenticateUser(request.username, request.password)
        } catch (e: Exception) {
            logger.error("LMS authentication error: ${e.message}")
            mapOf("error" to "LMS_UNAVAILABLE", "message" to e.message)
        }
        
        if (lmsResult is Map<*, *>) {
            val errorMsg = (lmsResult["message"] as? String) ?: "LMS error"
            logger.warn("LMS error for user ${request.username}: $errorMsg")
            throw org.springframework.security.access.AccessDeniedException(errorMsg)
        }
        
        if (lmsResult !is EdxAuthenticatedSession) {
            logger.warn("LMS validation failed for user: ${request.username}")
            throw org.springframework.security.access.AccessDeniedException("Invalid LMS credentials")
        }
        
        // Get or create user in our DB
        val user = userService.authenticateOrCreateOpenedxUser(request.username)
            ?: throw org.springframework.security.access.AccessDeniedException("Invalid user")
        
        val token = jwtTokenService.generateStaffToken(user.username, setOf("OPENEDX_USER"))
        
        return LoginResponseDto(
            accessToken = token,
            tokenType = "Bearer",
            expiresIn = 3600,
            username = user.username,
            role = "OPENEDX_USER"
        )
    }
}
