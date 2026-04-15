package soaringLizards.blps.config

import com.nimbusds.jose.jwk.source.ImmutableSecret
import com.nimbusds.jose.jwk.source.JWKSource
import com.nimbusds.jose.proc.SecurityContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.configuration.OAuth2AuthorizationServerConfiguration
import org.springframework.security.oauth2.core.AuthorizationGrantType
import org.springframework.security.oauth2.core.ClientAuthenticationMethod
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.server.authorization.client.InMemoryRegisteredClientRepository
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings
import org.springframework.security.oauth2.server.authorization.settings.OAuth2TokenFormat
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings
import java.time.Duration
import java.time.temporal.ChronoUnit
import javax.crypto.spec.SecretKeySpec

@Configuration
class AuthorizationServerConfig(
    val authorizationServerProperties: AuthorizationServerProperties,
) {
    @Bean
    fun registeredClientRepository() =
        InMemoryRegisteredClientRepository(
            RegisteredClient.withId(authorizationServerProperties.clientId)
                .clientName(authorizationServerProperties.clientName)
                .clientId(authorizationServerProperties.clientId)
                .clientSecret(authorizationServerProperties.clientSecret)
                .redirectUri(authorizationServerProperties.redirectUri)
                .scopes { it.addAll(authorizationServerProperties.scopes) }
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_JWT)
                .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
                .tokenSettings(
                    TokenSettings.builder()
                        .accessTokenFormat(OAuth2TokenFormat.SELF_CONTAINED)
                        .accessTokenTimeToLive(Duration.of(30, ChronoUnit.MINUTES))
                        .refreshTokenTimeToLive(Duration.of(120, ChronoUnit.MINUTES))
                        .reuseRefreshTokens(false)
                        .authorizationCodeTimeToLive(Duration.of(30, ChronoUnit.SECONDS))
                        .build(),
                )
                .build(),
        )

@Bean
    fun authorizationServerSettings() =
        AuthorizationServerSettings.builder()
            .issuer(authorizationServerProperties.issuerUrl)
            .tokenIntrospectionEndpoint(authorizationServerProperties.introspectionEndpoint)
            .tokenEndpoint("/oauth2/token")
            .build()


    @Bean
    fun jwkSource(): JWKSource<SecurityContext> {
        val secretKey = SecretKeySpec(
            authorizationServerProperties.jwtSecret.toByteArray(),
            "HMAC",
        )
        return ImmutableSecret(secretKey)
    }

    @Bean
    fun jwtDecoder(jwkSource: JWKSource<SecurityContext>): JwtDecoder =
        OAuth2AuthorizationServerConfiguration.jwtDecoder(jwkSource)
}
