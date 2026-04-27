package soaringLizards.blps.config.userDetails

import org.springframework.security.authentication.AuthenticationProvider
import org.springframework.security.core.Authentication
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.stereotype.Component

@Component
class JwtUserDetailsAuthenticationProvider : AuthenticationProvider {

    override fun authenticate(authentication: Authentication): Authentication? {
        val jwt = authentication as? JwtAuthenticationToken ?: return null

        val userDetails = jwt.token.toJwtUserDetails()

        return JwtUserDetailsAuthenticationToken(userDetails, userDetails.authorities)
    }

    override fun supports(authentication: Class<*>): Boolean {
        return authentication == JwtAuthenticationToken::class.java
    }
}
