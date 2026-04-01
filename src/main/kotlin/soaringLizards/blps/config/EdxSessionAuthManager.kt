package soaringLizards.blps.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.util.LinkedMultiValueMap
import org.springframework.web.client.RestClient
import tools.jackson.databind.ObjectMapper
import tools.jackson.module.kotlin.jacksonObjectMapper
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.util.Base64
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

data class EdxAuthenticatedSession(
    val cookieHeader: String,
    val csrfToken: String,
    val expiresAt: Instant?,
)

@Component
class EdxSessionAuthManager(
    @Suppress("HttpUrlsUsage")
    @Value("\${edx.api.base-url:http://local.openedx.io/api}")
    private val baseUrl: String,
    @Value("\${edx.api.username:}")
    private val username: String,
    @Value("\${edx.api.password:}")
    private val password: String,
) {
    private val objectMapper: ObjectMapper = jacksonObjectMapper()

    private val authClient: RestClient by lazy {
        RestClient.builder()
            .baseUrl(baseUrl.trimEnd('/'))
            .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
            .build()
    }

    private val lock = ReentrantLock()

    @Volatile
    private var cachedSession: EdxAuthenticatedSession? = null

    fun getSession(): EdxAuthenticatedSession? {
        if (username.isBlank() || password.isBlank())
            return null

        cachedSession?.takeIf(::isValid)?.let { return it }

        return lock.withLock {
            cachedSession?.takeIf(::isValid)?.let { return@withLock it }
            authenticate().also { cachedSession = it }
        }
    }

    private fun isValid(session: EdxAuthenticatedSession): Boolean =
        session.expiresAt?.isAfter(Instant.now().plusSeconds(60)) ?: true

    private fun authenticate(): EdxAuthenticatedSession {
        val cookies = linkedMapOf<String, String>()

        authClient.get()
            .uri("/user/v1/account/login_session/")
            .exchange { _, response ->
                mergeCookies(cookies, response.headers[HttpHeaders.SET_COOKIE].orEmpty())

                if (response.statusCode.isError) {
                    val body = response.body.readAllBytes().toString(StandardCharsets.UTF_8)
                    error("Failed to initialize Open edX login session: $body")
                }
            }

        val csrfToken = cookies["csrftoken"]
            ?: error("Open edX login initialization did not return csrftoken")

        val formData = LinkedMultiValueMap<String, String>().apply {
            add("email_or_username", username)
            add("password", password)
        }

        val loginResponse = authClient.post()
            .uri("/user/v2/account/login_session/")
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .header("X-CSRFToken", csrfToken)
            .header(HttpHeaders.COOKIE, buildCookieHeader(cookies))
            .body(formData)
            .exchange { _, response ->
                val body = response.body.readAllBytes().toString(StandardCharsets.UTF_8)
                mergeCookies(cookies, response.headers[HttpHeaders.SET_COOKIE].orEmpty())

                if (response.statusCode.isError)
                    error("Open edX login failed: $body")

                body
            }

        val loginJson = objectMapper.readTree(loginResponse)
        if (!loginJson.path("success").asBoolean(false)) {
            val message = loginJson.path("value").asText("unknown authentication error")
            error("Open edX login failed: $message")
        }

        val refreshedCsrfToken = cookies["csrftoken"]
            ?: error("Authenticated Open edX session did not return csrftoken")

        return EdxAuthenticatedSession(
            cookieHeader = buildCookieHeader(cookies),
            csrfToken = refreshedCsrfToken,
            expiresAt = extractExpiry(cookies["edx-jwt-cookie-header-payload"]),
        )
    }

    private fun mergeCookies(cookies: MutableMap<String, String>, setCookieHeaders: List<String>) {
        setCookieHeaders.forEach { header ->
            val cookiePair = header.substringBefore(';')
            val separatorIndex = cookiePair.indexOf('=')
            if (separatorIndex <= 0)
                return@forEach

            val name = cookiePair.take(separatorIndex)
            val value = cookiePair.substring(separatorIndex + 1)
            cookies[name] = value
        }
    }

    private fun buildCookieHeader(cookies: Map<String, String>): String =
        cookies.entries.joinToString("; ") { (name, value) -> "$name=$value" }

    private fun extractExpiry(headerPayload: String?): Instant? {
        if (headerPayload.isNullOrBlank())
            return null

        val jwtParts = headerPayload.split('.')
        if (jwtParts.size < 2)
            return null

        return runCatching {
            val payloadBytes = Base64.getUrlDecoder().decode(jwtParts[1])
            val payload = objectMapper.readTree(payloadBytes)
            payload.path("exp")
                .takeIf { !it.isMissingNode && it.canConvertToLong() }
                ?.asLong()
                ?.let(Instant::ofEpochSecond)
        }.getOrNull()
    }
}
