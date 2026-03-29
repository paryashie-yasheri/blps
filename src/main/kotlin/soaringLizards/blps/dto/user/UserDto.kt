package soaringLizards.blps.dto.user

data class UserDto(
    val username: String,
    val password: String,
    val email: Email,
    val role: String,
)
