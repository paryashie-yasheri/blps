package soaringLizards.blps.auth

import io.jsonwebtoken.Claims
import io.jsonwebtoken.JwtException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import io.jsonwebtoken.security.Keys
import net.logstash.logback.argument.StructuredArguments.kv
import org.slf4j.LoggerFactory
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.stereotype.Service
import soaringLizards.blps.config.AuthJwtProperties
import java.nio.charset.StandardCharsets
import java.util.*

@Service
class JwtService(
    private val props: AuthJwtProperties,
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    init {
        require(props.secret.length >= 32) { "auth.jwt.secret must be at least 32 characters" }
    }

    private val signingKey = Keys.hmacShaKeyFor(props.secret.toByteArray(StandardCharsets.UTF_8))
    private val parser = Jwts.parserBuilder().setSigningKey(signingKey).build()

    fun generateToken(subject: String, roles: List<String>): JwtToken {
        val now = Date()
        val expiresAt = Date(now.time + props.expirationMs)
        val token = Jwts.builder()
            .setSubject(subject)
            .claim("roles", roles)
            .setIssuedAt(now)
            .setExpiration(expiresAt)
            .signWith(signingKey, SignatureAlgorithm.HS256)
            .compact()
        return JwtToken(token, expiresAt.time)
    }

    fun generateRoomJoinToken(
        userId: String,
        roomId: String,
        roomRole: String,
        roomMemberId: String?,
        isMuted: Boolean,
    ): JwtToken {
        val now = Date()
        val expiresAt = Date(now.time + props.joinExpirationMs)
        val token = Jwts.builder()
            .setSubject(userId)
            .claim("tokenType", "room_join")
            .claim("roomId", roomId)
            .claim("roomRole", roomRole)
            .claim("roomMemberId", roomMemberId)
            .claim("muted", isMuted)
            .setIssuedAt(now)
            .setExpiration(expiresAt)
            .signWith(signingKey, SignatureAlgorithm.HS256)
            .compact()
        return JwtToken(token, expiresAt.time)
    }

    fun parseAuthentication(token: String): UsernamePasswordAuthenticationToken? {
        val claims = parseClaims(token) ?: return null
        val subject = claims.subject ?: return null
        val authorities = flattenRoles(claims["roles"]).map { SimpleGrantedAuthority(it) }
        return UsernamePasswordAuthenticationToken(subject, null, authorities)
    }

    private fun parseClaims(token: String): Claims? {
        return try {
            parser.parseClaimsJws(token).body
        } catch (exception: JwtException) {
            logger.debug("jwt_parse_failed", kv("reason", exception::class.simpleName))
            null
        }
    }

    private fun flattenRoles(value: Any?): List<String> =
        when (value) {
            is Collection<*> -> value.mapNotNull { it?.toString() }
            is Array<*> -> value.mapNotNull { it?.toString() }
            null -> emptyList()
            else -> listOf(value.toString())
        }
}

data class JwtToken(
    val token: String,
    val expiresAt: Long,
)