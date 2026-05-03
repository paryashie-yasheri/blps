package soaringLizards.blps.config.userDetails

import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.oauth2.core.OAuth2AuthenticationException
import org.springframework.security.oauth2.core.OAuth2Error
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.oauth2.jwt.Jwt
import soaringLizards.blps.dto.edx.EdxAuthenticatedSession
import soaringLizards.blps.dto.user.Email
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
    val username = subject
    val email = Email(getClaimAsString("email"))
    val uuid = getClaimAsString("user-id")
        ?.let(UUID::fromString)
        ?: throw invalidToken()

    val roles = getClaimAsStringList("roles") ?: listOf("OPENEDX_USER")
    val role = roles.firstOrNull() ?: "OPENEDX_USER"
    val authorities = roles.map { SimpleGrantedAuthority("ROLE_$it") }


    val edxSession =
        if (role == "OPENEDX_USER")
            EdxAuthenticatedSession(
                getClaimAsString("edxCookieHeader") ?: throw invalidToken(),
                getClaimAsString("edxCsrfToken") ?: throw invalidToken(),
                expiresAt ?: throw invalidToken(),
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

private fun invalidToken(): OAuth2AuthenticationException =
    OAuth2AuthenticationException(OAuth2Error("invalid_token"))
