package soaringLizards.blps.config.userDetails

import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.oauth2.jwt.Jwt
import soaringLizards.blps.dto.edx.EdxAuthenticatedSession
import soaringLizards.blps.dto.user.Email
import soaringLizards.blps.exception.BusinessLogicException
import java.util.*

class JwtUserDetails(
    val id: UUID,
    private val _username: String,
    val email: Email,
    val role: String,
    val token: String,
    private val authorities: Collection<GrantedAuthority> = listOf(SimpleGrantedAuthority("ROLE_$role")),
    val edxSession: EdxAuthenticatedSession?,
) : UserDetails {

    override fun getUsername(): String = _username

    override fun getAuthorities(): Collection<GrantedAuthority> = authorities

    override fun getPassword(): String? = null

    override fun isAccountNonExpired(): Boolean = true

    override fun isAccountNonLocked(): Boolean = true

    override fun isCredentialsNonExpired(): Boolean = true

    override fun isEnabled(): Boolean = true
}

fun Jwt.toJwtUserDetails(): JwtUserDetails {
    val email = Email(getClaimAsString("email") ?: throw BusinessLogicException("User email claim is missing"))
    val uuid = UUID.fromString(subject ?: throw BusinessLogicException("User id claim is missing"))
    val username = getClaimAsString("preferred_username")
        ?: throw BusinessLogicException("User preferred_username claim is missing")

    val roles =
        getClaimAsStringList("roles") ?: throw BusinessLogicException("Roles claim is missing")
    val role = roles.firstOrNull() ?: "OPENEDX_USER"
    val authorities = roles.map { SimpleGrantedAuthority("ROLE_$it") }


    val edxSession =
        if (role == "OPENEDX_USER")
            EdxAuthenticatedSession(
                getClaimAsString("edxCookieHeader"),
                getClaimAsString("edxCsrfToken"),
                expiresAt!!,
            )
        else
            null


    return JwtUserDetails(
        id = uuid,
        _username = username,
        role = role,
        email = email,
        token = tokenValue,
        authorities = authorities,
        edxSession = edxSession,
    )
}
