package soaringLizards.blps.service

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import soaringLizards.blps.dto.edx.EdxAuthenticatedSession
import soaringLizards.blps.dto.user.Email
import soaringLizards.blps.dto.user.UserResponse
import soaringLizards.blps.exception.BusinessLogicException
import java.security.Key
import java.time.Instant
import java.util.*

@Service
class JwtTokenService(
    @Value($$"${app.jwt.staff.secret}")
    val staffJwtSecret: String,

    @Value($$"${app.jwt.staff.issuer:blps-internal}")
    val staffJwtIssuer: String,

    @Value($$"${app.jwt.staff.expiration-minutes:60}")
    val staffJwtExpirationMinutes: Long,
) {

    private val key: Key by lazy {
        Keys.hmacShaKeyFor(staffJwtSecret.toByteArray())
    }

    fun generateToken(
        inHouseId: UUID,
        username: String,
        email: Email,
        roles: Set<String>,
        edxSession: EdxAuthenticatedSession? = null,
    ): String {
        val now = Date()

        val expiresInSeconds = staffJwtExpirationMinutes * 60

        val expiration = edxSession?.expiresAt ?: Instant.now().plusSeconds(expiresInSeconds)

        return Jwts.builder()
            .setSubject(username)
            .setIssuer(staffJwtIssuer)
            .setIssuedAt(now)
            .setExpiration(Date.from(expiration))
            .claim("user-id", inHouseId)
            .claim("roles", roles)
            .claim("email", email.value)
            .claim("edxCookieHeader", edxSession?.cookieHeader)
            .claim("edxCsrfToken", edxSession?.csrfToken)
            .signWith(key, SignatureAlgorithm.HS256)
            .compact()
    }

    fun validateToken(token: String): Boolean {
        return try {
            Jwts.parserBuilder()
                .setSigningKey(key)
                .requireIssuer(staffJwtIssuer)
                .build()
                .parseClaimsJws(token)
            true
        } catch (e: Exception) {
            false
        }
    }

    fun extractUserResponseFromToken(token: String): UserResponse? {
        return try {
            val claims = Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .body

            val username = claims.subject
            val email = claims.get("email", String::class.java)
            val role = claims.get("roles", List::class.java)
                ?.filterIsInstance<String>()
                ?.first()
                ?: throw BusinessLogicException("bad roles in token")

            UserResponse(
                username,
                Email(email),
                role,
            )

        } catch (e: Exception) {
            null
        }
    }

    fun extractRolesFromToken(token: String): Set<String> {
        return try {
            val claims = Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .body
            val roles = claims.get("roles", List::class.java)
            roles?.filterIsInstance<String>()?.toSet() ?: emptySet()
        } catch (e: Exception) {
            emptySet()
        }
    }
}
