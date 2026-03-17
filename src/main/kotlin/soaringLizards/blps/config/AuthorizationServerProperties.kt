import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "spring.security.oauth2.authorization-server")
data class AuthorizationServerProperties(
    val issuerUrl: String,
    val introspectionEndpoint: String,
);
