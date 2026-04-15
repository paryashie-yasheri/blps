package soaringLizards.blps.dto.user

import soaringLizards.blps.jooq.generated.tables.records.UsersRecord

class UserResponse(
    val username: String,
    val email: Email,
    val role: String,
)

class LoginResponseDto(
    val accessToken: String,
    val tokenType: String,
    val expiresIn: Long,
    val username: String,
    val role: String,
)

fun UsersRecord.toUserResponse() =
    UserResponse(
        username!!,
        Email(email!!),
        role!!,
    )
