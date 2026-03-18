package soaringLizards.blps.dto.user

import soaringLizards.blps.jooq.generated.tables.records.UsersRecord
import java.util.UUID

data class UserDto(
    val username: String,
    val password: String,
    val email: Email,
    val role: String,
)
