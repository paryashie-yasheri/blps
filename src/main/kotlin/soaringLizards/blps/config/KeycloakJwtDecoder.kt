package soaringLizards.blps.config

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Primary
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder
import org.springframework.stereotype.Component

@Component
@Primary
class KeycloakJwtDecoder(
    @Value("\${keycloak.server-url}") private val keycloakServerUrl: String,
    @Value("\${keycloak.master.realm}") private val masterRealm: String,
) : JwtDecoder {
    private val logger = LoggerFactory.getLogger(KeycloakJwtDecoder::class.java)
    private var cachedDecoder: NimbusJwtDecoder? = null
    private var lastFetchTime: Long = 0
    private val cacheValidityMs = 3600000L

    override fun decode(token: String): Jwt {
        val decoder = getDecoder()
        return decoder.decode(token)
    }

    private fun getDecoder(): NimbusJwtDecoder {
        val now = System.currentTimeMillis()
        if (cachedDecoder != null && now - lastFetchTime < cacheValidityMs) {
            return cachedDecoder!!
        }

        val issuer = "$keycloakServerUrl/realms/$masterRealm"
        logger.info("Creating JWKS decoder with issuer: $issuer")

        val decoder = NimbusJwtDecoder.withIssuerLocation(issuer).build()
        cachedDecoder = decoder
        lastFetchTime = now
        logger.info("JWKS decoder updated")

        return decoder
    }
}
