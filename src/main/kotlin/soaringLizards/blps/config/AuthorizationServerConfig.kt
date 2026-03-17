import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configurers.ExceptionHandlingConfigurer
import org.springframework.security.oauth2.core.AuthorizationGrantType
import org.springframework.security.oauth2.core.ClientAuthenticationMethod
import org.springframework.security.oauth2.server.authorization.client.InMemoryRegisteredClientRepository
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings
import org.springframework.security.oauth2.server.authorization.settings.OAuth2TokenFormat
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint
import java.time.Duration
import java.time.temporal.ChronoUnit


@Configuration(proxyBeanMethods = false)
class AuthorizationServerConfig(
    val authorizationServerProperties: AuthorizationServerProperties,
) {
    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    @Throws(Exception::class)
    fun authServerSecurityFilterChain(http: HttpSecurity) =
        http.exceptionHandling { exceptions: ExceptionHandlingConfigurer<HttpSecurity?>? ->
            exceptions!!.authenticationEntryPoint(
                LoginUrlAuthenticationEntryPoint("/login")
            )
        }.build()

    @Bean
    fun registeredClientRepository() =
        InMemoryRegisteredClientRepository(
            RegisteredClient.withId("blps-client-id")
                .clientName("blps client")
                .clientId("blps-client")
                .clientSecret("{noop}blps-client")
                .redirectUri("http://127.0.0.1:8080/code")
                .scope("read.scope")
                .scope("write.scope")
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
                        .build()
                )
                .build()
        )

    @Bean
    fun authorizationServerSettings() =
        AuthorizationServerSettings.builder()
            .issuer(authorizationServerProperties.issuerUrl)
            .tokenIntrospectionEndpoint(authorizationServerProperties.introspectionEndpoint)
            .build()
}
