package soaringLizards.blps.config

import org.springframework.security.authentication.AbstractAuthenticationToken
import org.springframework.security.core.GrantedAuthority

class JwtUserDetailsAuthenticationToken(
    principal: JwtUserDetails,
    authorities: Collection<GrantedAuthority>
) : AbstractAuthenticationToken(authorities) {

    private val principal: JwtUserDetails = principal

    init {
        isAuthenticated = true
    }

    override fun getCredentials(): String? = principal.token

    override fun getPrincipal(): JwtUserDetails = principal
}
