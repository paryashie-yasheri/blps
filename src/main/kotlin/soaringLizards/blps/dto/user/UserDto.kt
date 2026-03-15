package soaringLizards.blps.dto.user

import java.util.UUID

data class UserDto(
    val id: UUID,
    val username: String,
    val role: Roles,
    val email: String, // TODO: add value string class "email" for automatic validation etc
)
