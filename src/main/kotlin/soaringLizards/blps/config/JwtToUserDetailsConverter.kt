package soaringLizards.blps.config

import org.springframework.core.convert.converter.Converter
import org.springframework.security.authentication.AbstractAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.stereotype.Component

@Component
class JwtToUserDetailsConverter : Converter<Jwt, AbstractAuthenticationToken> {

    override fun convert(jwt: Jwt): AbstractAuthenticationToken {
        val username = jwt.subject
        val roles = jwt.getClaimAsStringList("roles") ?: listOf("OPENEDX_USER")
        val role = roles.firstOrNull() ?: "OPENEDX_USER"

        val userDetails = JwtUserDetails(
            _username = username,
            role = role,
            token = jwt.tokenValue
        )

        val authorities = roles.map { SimpleGrantedAuthority("ROLE_$it") }
        return JwtUserDetailsAuthenticationToken(userDetails, authorities)
    }
}
