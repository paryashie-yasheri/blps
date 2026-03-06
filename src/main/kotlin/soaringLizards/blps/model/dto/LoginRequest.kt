package soaringLizards.blps.model.dto

import com.fasterxml.jackson.annotation.JsonAlias
import io.swagger.v3.oas.annotations.media.Schema

@Schema(name = "LoginRequest", description = "Credentials for login")
data class LoginRequest(
    @field:JsonAlias("userId", "username", "email")
    @field:Schema(
        description = "Username or email (legacy: userId)",
        example = "test@example.com"
    )
    val login: String,
    @field:Schema(description = "Password", format = "password", example = "test")
    val password: String,
)