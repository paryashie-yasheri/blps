package soaringLizards.blps.config.userDetails

import org.springframework.core.convert.converter.Converter
import org.springframework.security.authentication.AbstractAuthenticationToken
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.stereotype.Component

@Component
class JwtToAbstractAuthenticationTokenConverter : Converter<Jwt, AbstractAuthenticationToken> {
    override fun convert(jwt: Jwt): AbstractAuthenticationToken {
        val userDetails = jwt.toJwtUserDetails()
        return JwtUserDetailsAuthenticationToken(userDetails, userDetails.authorities)
    }
}
