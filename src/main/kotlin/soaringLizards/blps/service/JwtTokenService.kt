package soaringLizards.blps.service

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.security.Key
import java.util.*

@Service
class JwtTokenService(
    @Value("\${app.jwt.staff.secret}")
    private val staffJwtSecret: String,

    @Value("\${app.jwt.staff.issuer:blps-internal}")
    private val staffJwtIssuer: String,

    @Value("\${app.jwt.staff.expiration-minutes:60}")
    private val staffJwtExpirationMinutes: Long,
) {

    private val key: Key by lazy {
        Keys.hmacShaKeyFor(staffJwtSecret.toByteArray())
    }

    fun generateStaffToken(username: String, roles: Set<String>): String {
        val now = Date()
        val expiration = Date(now.time + staffJwtExpirationMinutes * 60 * 1000)

        return Jwts.builder()
            .setSubject(username)
            .setIssuer(staffJwtIssuer)
            .setIssuedAt(now)
            .setExpiration(expiration)
            .claim("roles", roles)
            .signWith(key, SignatureAlgorithm.HS256)
            .compact()
    }

    fun validateStaffToken(token: String): Boolean {
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

    fun extractUsernameFromToken(token: String): String? {
        return try {
            val claims = Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .body
            claims.subject
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
            (roles as? List<*>)?.filterIsInstance<String>()?.toSet() ?: emptySet()
        } catch (e: Exception) {
            emptySet()
        }
    }
}
