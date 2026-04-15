package soaringLizards.blps.dto.user

data class RegisterDto(
    val username: String,
    val password: String,
    val email: Email,
)

data class LoginRequestDto(
    val username: String,
    val password: String,
)
