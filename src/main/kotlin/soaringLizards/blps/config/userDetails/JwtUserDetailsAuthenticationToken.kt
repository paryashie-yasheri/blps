package soaringLizards.blps.config.userDetails

import org.springframework.security.authentication.AbstractAuthenticationToken
import org.springframework.security.core.GrantedAuthority

class JwtUserDetailsAuthenticationToken(
    private val principal: JwtUserDetails,
    authorities: Collection<GrantedAuthority>,
) : AbstractAuthenticationToken(authorities) {

    init {
        isAuthenticated = true
    }

    override fun getCredentials(): String = principal.token

    override fun getPrincipal(): JwtUserDetails = principal
}
