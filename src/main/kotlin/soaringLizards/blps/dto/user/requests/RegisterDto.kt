package soaringLizards.blps.dto.user.requests

data class RegisterDto(
    val username: String,
    val password: String,
    val email: String, // TODO: add value string class "email" for automatic validation etc
)
