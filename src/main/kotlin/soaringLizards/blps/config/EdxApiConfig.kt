package soaringLizards.blps.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.client.RestClient

@Configuration
class EdxApiConfig {

    @Suppress("HttpUrlsUsage")
    @Value("\${edx.api.base-url:http://local.openedx.io/api}")
    private lateinit var baseUrl: String

    @Value("\${edx.api.authorization:}")
    private lateinit var authorizationHeader: String

    @Value("\${edx.api.cookie:}")
    private lateinit var cookieHeader: String

    @Value("\${edx.api.csrf-token:}")
    private lateinit var csrfToken: String

    @Bean
    fun edxRestClient(edxSessionAuthManager: EdxSessionAuthManager): RestClient {
        return RestClient.builder()
            .baseUrl(baseUrl.trimEnd('/'))
            .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
            .requestInterceptor { request, body, execution ->
                val configuredAuthorization = authorizationHeader.trim().takeIf { it.isNotEmpty() }
                val configuredCookie = cookieHeader.trim().takeIf { it.isNotEmpty() }
                val configuredCsrfToken = csrfToken.trim().takeIf { it.isNotEmpty() }
                val authenticatedSession =
                    if (
                        configuredAuthorization == null &&
                        configuredCookie == null
                    )
                        edxSessionAuthManager.getSession()
                    else
                        null


                request.headers.getFirst(HttpHeaders.AUTHORIZATION)
                    ?: when {
                        configuredAuthorization != null -> request.headers.set(
                            HttpHeaders.AUTHORIZATION,
                            configuredAuthorization,
                        )

                        else -> extractToken()?.let(request.headers::setBearerAuth)
                    }

                request.headers.getFirst(HttpHeaders.COOKIE) ?: {
                    when {
                        configuredCookie != null -> request.headers.set(HttpHeaders.COOKIE, configuredCookie)
                        authenticatedSession != null -> request.headers.set(
                            HttpHeaders.COOKIE,
                            authenticatedSession.cookieHeader,
                        )
                    }
                }

                val method = request.method
                if (
                    method != HttpMethod.GET &&
                    method != HttpMethod.HEAD &&
                    method != HttpMethod.OPTIONS &&
                    request.headers.getFirst("X-CSRFToken") == null
                ) {
                    when {
                        configuredCsrfToken != null -> request.headers.set("X-CSRFToken", configuredCsrfToken)
                        authenticatedSession != null -> request.headers.set(
                            "X-CSRFToken",
                            authenticatedSession.csrfToken,
                        )
                    }
                }

                execution.execute(request, body)
            }
            .build()
    }

    private fun extractToken(): String? {
        val authentication: Authentication? = SecurityContextHolder.getContext().authentication
        return when (val principal = authentication?.principal) {
            is Jwt -> principal.tokenValue
            else -> null
        }
    }
}
