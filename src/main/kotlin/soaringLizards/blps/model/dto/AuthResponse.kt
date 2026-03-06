package soaringLizards.blps.model.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(name = "AuthResponse", description = "JWT access token response")
data class AuthResponse(
    @field:Schema(description = "JWT access token", example = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ0ZXN0In0.signature")
    val accessToken: String,
    @field:Schema(description = "Expiration time (epoch millis)", example = "1734123456789")
    val expiresAt: Long,
)