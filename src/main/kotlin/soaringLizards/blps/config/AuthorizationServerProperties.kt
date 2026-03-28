package soaringLizards.blps.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "spring.security.oauth2.authorization-server")
data class AuthorizationServerProperties(
    val issuerUrl: String,
    val introspectionEndpoint: String,
    val jwtSecret: String,
    val scopes: List<String>,
    val redirectUri: String,
    val clientSecret: String,
    val clientName: String,
    val clientId: String,
)
