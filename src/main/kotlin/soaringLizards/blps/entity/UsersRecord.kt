package soaringLizards.blps.entity

import soaringLizards.blps.dto.user.Email
import java.util.*

data class UsersRecord(
    val id: UUID,
    val email: Email,
    val username: String,
    val role: String,
)

