package soaringLizards.blps.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
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
    fun edxRestClient(edxSessionAuthManager: EdxSessionAuthManager) = RestClient.builder()
        .baseUrl(baseUrl.trimEnd('/'))
        .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
        .requestInterceptor { request, body, execution ->
            val configuredAuthorization = authorizationHeader.trim().takeIf { it.isNotEmpty() }
            val configuredCookie = cookieHeader.trim().takeIf { it.isNotEmpty() }
            val configuredCsrfToken = csrfToken.trim().takeIf { it.isNotEmpty() }

            if (configuredAuthorization != null) {
                request.headers.set(HttpHeaders.AUTHORIZATION, configuredAuthorization)
            } else if (configuredCookie != null) {
                request.headers.set(HttpHeaders.COOKIE, configuredCookie)
                if (configuredCsrfToken != null) {
                    request.headers.set("X-CSRFToken", configuredCsrfToken)
                }
            } else {
                val authenticatedSession = edxSessionAuthManager.getSession()
                if (authenticatedSession != null) {
                    request.headers.set(HttpHeaders.COOKIE, authenticatedSession.cookieHeader)
                    request.headers.set("X-CSRFToken", authenticatedSession.csrfToken)
                }
            }

            execution.execute(request, body)
        }
        .build()
}
