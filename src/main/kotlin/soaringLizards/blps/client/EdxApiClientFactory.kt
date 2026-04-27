package soaringLizards.blps.client

import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import soaringLizards.blps.config.EdxSessionAuthManager
import soaringLizards.blps.dto.edx.EdxAuthenticatedSession

@Component
class EdxApiClientFactory(
    @Value("\${edx.api.base-url}") private val baseUrl: String,
    private val edxSessionAuthManager: EdxSessionAuthManager,
) {
    fun createForUser(session: EdxAuthenticatedSession): RestClient {
        return RestClient.builder()
            .baseUrl(baseUrl)
            .defaultHeader(HttpHeaders.COOKIE, session.cookieHeader)
            .defaultHeader("X-CSRFToken", session.csrfToken)
            .build()
    }

    fun createForService(): RestClient {
        return RestClient.builder()
            .baseUrl(baseUrl)
            .build()
    }
}
