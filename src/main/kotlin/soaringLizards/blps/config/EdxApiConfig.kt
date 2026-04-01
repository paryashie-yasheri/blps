package soaringLizards.blps.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.client.RestClient

@Configuration
class EdxApiConfig {

    @Suppress("HttpUrlsUsage")
    @Value("\${edx.api.base-url:http://local.openedx.io}")
    private lateinit var baseUrl: String

    @Bean
    fun edxRestClient(): RestClient {
        return RestClient.builder()
            .baseUrl(baseUrl.trimEnd('/'))
            .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
            .requestInterceptor { request, body, execution ->
                request.headers.getFirst(HttpHeaders.AUTHORIZATION)
                    ?: extractToken()?.let(request.headers::setBearerAuth)

                execution.execute(request, body)
            }
            .build()
    }

    private fun extractToken(): String? {
        val authentication: Authentication? = SecurityContextHolder.getContext().authentication
        return when (val principal = authentication?.principal) {
            is Jwt -> principal.tokenValue
            is String -> principal
            else -> null
        }
    }
}
