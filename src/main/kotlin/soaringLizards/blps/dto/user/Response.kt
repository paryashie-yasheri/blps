package soaringLizards.blps.dto.user

import com.fasterxml.jackson.annotation.JsonProperty
import soaringLizards.blps.config.userDetails.JwtUserDetails
import soaringLizards.blps.entity.UsersRecord

class UserResponse(
    val username: String,
    val email: Email,
    val role: String,
)

class LoginResponseDto(
    @JsonProperty("access_token")
    val accessToken: String,
    @JsonProperty("token_type")
    val tokenType: String,
    @JsonProperty("expires_in")
    val expiresIn: Long,
    @JsonProperty("refresh_token")
    val refreshToken: String?,
    @JsonProperty("username")
    val username: String,
    @JsonProperty("role")
    val role: String,
)

fun UsersRecord.toUserResponse() =
    UserResponse(
        username,
        email,
        role,
    )

fun JwtUserDetails.toUserResponse() =
    UserResponse(
        username,
        email,
        role,
    )

class KeycloakTokenResponse(
    @JsonProperty("access_token")
    val accessToken: String,
    @JsonProperty("token_type")
    val tokenType: String,
    @JsonProperty("expires_in")
    val expiresIn: Long,
    @JsonProperty("refresh_token")
    val refreshToken: String?,
    @JsonProperty("scope")
    val scope: String?,
)

fun KeycloakTokenResponse.toLoginResponse(username: String, role: String) =
    LoginResponseDto(
        accessToken,
        tokenType,
        expiresIn,
        refreshToken,
        username = username,
        role = role,
    )
