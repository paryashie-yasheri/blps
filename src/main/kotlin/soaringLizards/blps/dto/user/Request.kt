package soaringLizards.blps.dto.user

data class RegisterDto(
    val username: String,
    val password: String,
    val email: Email,
)
