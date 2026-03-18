package soaringLizards.blps.dto.user

import java.util.UUID

data class UserDto(
    val username: String,
    val password: String,
    val email: Email,
    val role: String,
)


