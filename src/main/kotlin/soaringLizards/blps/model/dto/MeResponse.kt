package soaringLizards.blps.model.dto

import io.swagger.v3.oas.annotations.media.Schema

//import soaringLizards.conclave.model.user.Profile

@Schema(name = "CurrentUser", description = "Current authenticated user (without sensitive fields)")
data class CurrentUserResponse(
    @field:Schema(description = "User id", example = "test")
    val id: String,
    @field:Schema(description = "Email", example = "test@example.com")
    val email: String,
    @field:Schema(description = "Role", example = "user")
    val role: String,
    @field:Schema(description = "Phone", example = "+79990001122")
    val phone: String?,
)

@Schema(name = "MeResponse", description = "Current user with profile")
data class MeResponse(
    @field:Schema(description = "User account")
    val user: CurrentUserResponse,
    //@field:Schema(description = "User profile")
    //val profile: Profile,
)