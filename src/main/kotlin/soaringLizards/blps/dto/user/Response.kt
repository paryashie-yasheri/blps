package soaringLizards.blps.dto.user

import soaringLizards.blps.jooq.generated.tables.records.UsersRecord

class UserResponse(
    val username: String,
    val email: Email,
    val role: String,
)

fun UsersRecord.toUserResponse() = UserResponse(
    username!!,
    Email(email!!),
    role!!
)
