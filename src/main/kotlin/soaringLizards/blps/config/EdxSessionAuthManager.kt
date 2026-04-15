package soaringLizards.blps.config

import org.slf4j.LoggerFactory
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

private val logger = LoggerFactory.getLogger(EdxSessionAuthManager::class.java)

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
        logger.info("getSession called, username set: ${username.isNotBlank()}")
        
        if (username.isBlank() || password.isBlank()) {
            logger.warn("LMS credentials not configured")
            return null
        }

        cachedSession?.takeIf(::isValid)?.let { 
            logger.info("Using cached LMS session")
            return it 
        }

        return lock.withLock {
            cachedSession?.takeIf(::isValid)?.let { return@withLock it }
            logger.info("Authenticating with LMS...")
            authenticate().also { 
                cachedSession = it 
                logger.info("LMS authentication ${if (it != null) "SUCCESS" else "FAILED"}")
            }
        }
    }

    private fun isValid(session: EdxAuthenticatedSession): Boolean =
        session.expiresAt?.isAfter(Instant.now().plusSeconds(60)) ?: true

    private fun authenticate(): EdxAuthenticatedSession {
        logger.info("Starting LMS authentication...")
        
        val cookies = linkedMapOf<String, String>()

        authClient.get()
            .uri("/user/v1/account/login_session/")
            .exchange { _, response ->
                logger.info("LMS login init status: ${response.statusCode}")
                mergeCookies(cookies, response.headers[HttpHeaders.SET_COOKIE].orEmpty())

                if (response.statusCode.isError) {
                    val body = response.body.readAllBytes().toString(StandardCharsets.UTF_8)
                    logger.error("LMS login init failed: $body")
                    error("Failed to initialize Open edX login session: $body")
                }
            }

        val csrfToken = cookies["csrftoken"]
            ?: run {
                logger.error("LMS did not return csrftoken")
                error("Open edX login initialization did not return csrftoken")
            }
        
        logger.debug("Got CSRF token: ${csrfToken.take(10)}...")

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

                if (response.statusCode.isError) {
                    logger.error("LMS login failed: $body")
                    error("Open edX login failed: $body")
                }

                body
            }

        val loginJson = objectMapper.readTree(loginResponse)
        if (!loginJson.path("success").asBoolean(false)) {
            val message = loginJson.path("value").asText("unknown authentication error")
            logger.error("LMS login returned failure: $message")
            error("Open edX login failed: $message")
        }

        val refreshedCsrfToken = cookies["csrftoken"]
            ?: run {
                logger.error("LMS did not return csrftoken after login")
                error("Authenticated Open edX session did not return csrftoken")
            }

        logger.info("LMS authentication successful for user: $username")
        
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

fun authenticateUser(userEmail: String, password: String): Any {
        logger.info("Attempting LMS user authentication for: $userEmail")
        
        val cookies = linkedMapOf<String, String>()

        logger.info("Step 1: Getting LMS login session...")
        val initResponse = authClient.get()
            .uri("/user/v1/account/login_session/")
            .exchange { _, response ->
                logger.info("LMS init response status: ${response.statusCode}")
                mergeCookies(cookies, response.headers[HttpHeaders.SET_COOKIE].orEmpty())
                response.statusCode.isError
            }

        if (initResponse) {
            logger.warn("LMS init returned error")
            return mapOf("error" to "LMS_INIT_FAILED", "message" to "LMS initialization failed")
        }

        val csrfToken = cookies["csrftoken"]
        if (csrfToken == null) {
            logger.warn("LMS did not return csrftoken, cookies: ${cookies.keys}")
            return mapOf("error" to "NO_CSRF_TOKEN", "message" to "LMS did not return CSRF token")
        }
        logger.debug("Got CSRF token: ${csrfToken.take(10)}...")

        logger.info("Step 2: Posting login credentials...")
        val formData = LinkedMultiValueMap<String, String>().apply {
            add("email_or_username", userEmail)
            add("password", password)
        }

        return try {
            val loginResponse = authClient.post()
                .uri("/user/v2/account/login_session/")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .header("X-CSRFToken", csrfToken)
                .header(HttpHeaders.COOKIE, buildCookieHeader(cookies))
                .body(formData)
                .exchange { _, response ->
                    val body = response.body.readAllBytes().toString(StandardCharsets.UTF_8)
                    logger.info("LMS login response status: ${response.statusCode}, body: ${body.take(200)}")
                    mergeCookies(cookies, response.headers[HttpHeaders.SET_COOKIE].orEmpty())

                    if (response.statusCode.isError)
                        null
                    else
                        body
                }

            if (loginResponse == null) {
                logger.warn("LMS login returned HTTP error")
                return mapOf("error" to "LMS_LOGIN_FAILED", "message" to "LMS login HTTP error")
            }

            val loginJson = objectMapper.readTree(loginResponse)
            logger.info("LMS login response JSON: $loginJson")
            
            if (!loginJson.path("success").asBoolean(false)) {
                val errorCode = loginJson.path("error_code").asText("incorrect_credentials")
                val message = loginJson.path("value").asText("Authentication failed")
                logger.warn("LMS login returned failure: $message")
                return mapOf("error" to errorCode, "message" to message)
            }

            val refreshedCsrfToken = cookies["csrftoken"]
            if (refreshedCsrfToken == null) {
                logger.warn("LMS did not return csrftoken after login")
                return mapOf("error" to "NO_CSRF_AFTER_LOGIN", "message" to "LMS did not return CSRF token after login")
            }

            logger.info("LMS authentication SUCCESS for user: $userEmail")
            EdxAuthenticatedSession(
                cookieHeader = buildCookieHeader(cookies),
                csrfToken = refreshedCsrfToken,
                expiresAt = extractExpiry(cookies["edx-jwt-cookie-header-payload"]),
            )
        } catch (e: Exception) {
            logger.error("LMS authentication exception: ${e.message}", e)
            mapOf("error" to "LMS_EXCEPTION", "message" to e.message)
        }
    }
}
