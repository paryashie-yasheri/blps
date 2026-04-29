package soaringLizards.blps.config

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Primary
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder
import org.springframework.stereotype.Component
import java.time.Instant
import java.util.*

@Component
@Primary
class KeycloakJwtDecoder(
    @Value("\${keycloak.server-url}") private val keycloakServerUrl: String,
    @Value("\${keycloak.master.realm}") private val masterRealm: String,
    @Value("\${app.jwt.staff.secret}") private val localJwtSecret: String,
    @Value("\${app.jwt.staff.issuer:blps-internal}") private val localJwtIssuer: String,
) : JwtDecoder {
    private val logger = LoggerFactory.getLogger(KeycloakJwtDecoder::class.java)
    private val localKey = Keys.hmacShaKeyFor(localJwtSecret.toByteArray())
    private var cachedDecoder: NimbusJwtDecoder? = null
    private var lastFetchTime: Long = 0
    private val cacheValidityMs = 3600000L

    override fun decode(token: String): Jwt {
        return try {
            tryDecodeWithLocalKey(token)
        } catch (e: Exception) {
            logger.debug("Local JWT decoding failed: ${e.message}, trying Keycloak JWKS")
            try {
                tryDecodeWithJwks(token)
            } catch (jwksEx: Exception) {
                logger.warn("JWKS decoding failed: ${jwksEx.message}, attempting refresh")
                cachedDecoder = null
                tryDecodeWithJwks(token)
            }
        }
    }

    private fun tryDecodeWithLocalKey(token: String): Jwt {
        val claims = Jwts.parserBuilder()
            .setSigningKey(localKey)
            .requireIssuer(localJwtIssuer)
            .build()
            .parseClaimsJws(token)

        val body = claims.body
        val now = Date()
        val issuedAt = body.issuedAt?.toInstant() ?: Instant.now()
        val expiresAt = body.expiration?.toInstant() ?: Instant.now().plusSeconds(3600)

        val headerClaims = mutableMapOf<String, Any>(
            "alg" to "HS256",
            "typ" to "JWT",
        )

        val claimsMap = mutableMapOf<String, Any>()
        body.subject?.let { claimsMap["sub"] = it }
        body.issuer?.let { claimsMap["iss"] = it }
        body.get("preferred_username")?.let { claimsMap["preferred_username"] = it }
        body.get("roles")?.let { claimsMap["roles"] = it }
        body.get("email")?.let { claimsMap["email"] = it }
        body.get("edxCookieHeader")?.let { claimsMap["edxCookieHeader"] = it }
        body.get("edxCsrfToken")?.let { claimsMap["edxCsrfToken"] = it }

        logger.info("Token validated with local secret for user: ${body.subject}")
        return Jwt(token, issuedAt, expiresAt, headerClaims, claimsMap)
    }

    private fun tryDecodeWithJwks(token: String): Jwt {
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
