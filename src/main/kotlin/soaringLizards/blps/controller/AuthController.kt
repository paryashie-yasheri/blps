package soaringLizards.blps.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import soaringLizards.blps.error.ApiExceptionHandler
import soaringLizards.blps.model.dto.AuthResponse
import soaringLizards.blps.model.dto.LoginRequest
import soaringLizards.blps.model.dto.RegisterRequest
import soaringLizards.blps.service.AuthService
import io.swagger.v3.oas.annotations.tags.Tag as OpenApiTag

@RestController
@RequestMapping("/auth")
@OpenApiTag(name = "Auth", description = "Registration and authentication")
class AuthController(
    private val authService: AuthService,
) {

    @PostMapping("/login")
    @Operation(summary = "Login", description = "Authenticate by username/email and return JWT access token")
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "OK",
                content = [Content(schema = Schema(implementation = AuthResponse::class))]
            ),
            ApiResponse(
                responseCode = "401",
                description = "Invalid credentials",
                content = [Content(schema = Schema(implementation = ApiExceptionHandler.ApiError::class))]
            )
        ]
    )
    fun login(@RequestBody request: LoginRequest): ResponseEntity<AuthResponse> =
        ResponseEntity.ok(authService.authenticate(request.login, request.password))

    @PostMapping("/register")
    @Operation(summary = "Register", description = "Create a new user and return JWT access token")
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "OK",
                content = [Content(schema = Schema(implementation = AuthResponse::class))]
            ),
            ApiResponse(
                responseCode = "400",
                description = "Validation error",
                content = [Content(schema = Schema(implementation = ApiExceptionHandler.ApiError::class))]
            ),
            ApiResponse(
                responseCode = "409",
                description = "User already exists",
                content = [Content(schema = Schema(implementation = ApiExceptionHandler.ApiError::class))]
            )
        ]
    )
    fun register(@RequestBody request: RegisterRequest): ResponseEntity<AuthResponse> =
        ResponseEntity.ok(authService.register(request))
}