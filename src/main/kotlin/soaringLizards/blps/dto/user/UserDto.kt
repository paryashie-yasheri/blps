package soaringLizards.blps.dto.user

import java.util.UUID

data class UserDto(
    val username: String,
    val password: String,
    val email: String, // TODO: add value string class "email" for automatic validation etc
    val role: String,
)


