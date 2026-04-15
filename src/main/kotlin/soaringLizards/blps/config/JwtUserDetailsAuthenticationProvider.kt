package soaringLizards.blps.config

import org.springframework.security.authentication.AuthenticationProvider
import org.springframework.security.core.Authentication
import org.springframework.security.core.AuthenticationException
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.stereotype.Component

@Component
class JwtUserDetailsAuthenticationProvider : AuthenticationProvider {

    override fun authenticate(authentication: Authentication): Authentication? {
        val jwt = authentication as? JwtAuthenticationToken ?: return null
        
        val jwtToken = jwt.token
        val username = jwtToken.subject
        val roles = jwtToken.getClaimAsStringList("roles") ?: listOf("OPENEDX_USER")
        val role = roles.firstOrNull() ?: "OPENEDX_USER"

        val userDetails = JwtUserDetails(
            _username = username,
            role = role,
            token = jwtToken.tokenValue
        )

        return JwtUserDetailsAuthenticationToken(userDetails, userDetails.authorities)
    }

    override fun supports(authentication: Class<*>): Boolean {
        return authentication == JwtAuthenticationToken::class.java
    }
}
