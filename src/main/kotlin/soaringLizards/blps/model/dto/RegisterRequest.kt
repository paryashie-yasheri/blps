package soaringLizards.blps.model.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(name = "RegisterRequest", description = "User registration request")
data class RegisterRequest(
    @field:Schema(
        description = "Optional user id. If omitted, generated on server",
        example = "f2b1a9e8-3c5d-4d2a-9f40-2b7d5d04f7b1"
    )
    val userId: String? = null,
    @field:Schema(description = "Email", format = "email", example = "test@example.com")
    val email: String,
    @field:Schema(description = "Password", format = "password", example = "test")
    val password: String,
    @field:Schema(description = "Unique username", example = "test")
    val username: String,
    @field:Schema(description = "Display name", example = "Admin User")
    val displayName: String? = null,
    @field:Schema(description = "Phone number", example = "+79990000000")
    val phone: String? = null,
)