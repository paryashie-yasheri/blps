package soaringLizards.blps.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "auth.jwt")
data class AuthJwtProperties(
    var secret: String = "",
    var expirationMs: Long = 3_600_000,
    var joinExpirationMs: Long = 60_000,
)