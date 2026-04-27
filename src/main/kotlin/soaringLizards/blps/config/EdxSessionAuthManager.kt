package soaringLizards.blps.config

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.util.LinkedMultiValueMap
import org.springframework.web.client.RestClient
import soaringLizards.blps.dto.edx.EdxAuthenticatedSession
import soaringLizards.blps.exception.LmsException
import tools.jackson.databind.ObjectMapper
import tools.jackson.module.kotlin.jacksonObjectMapper
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.util.*

private val logger = LoggerFactory.getLogger(EdxSessionAuthManager::class.java)


@Component
class EdxSessionAuthManager(
    @Suppress("HttpUrlsUsage")
    @Value($$"${edx.api.base-url:http://local.openedx.io/api}")
    private val baseUrl: String,
) {
    private val objectMapper: ObjectMapper = jacksonObjectMapper()

    private val authClient: RestClient by lazy {
        RestClient.builder()
            .baseUrl(baseUrl.trimEnd('/'))
            .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
            .build()
    }

    private fun isValid(session: EdxAuthenticatedSession): Boolean =
        session.expiresAt.isAfter(Instant.now().plusSeconds(60)) ?: true

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

    fun authenticateUser(userEmail: String, password: String): EdxAuthenticatedSession {
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
            throw LmsException(mapOf("error" to "LMS_INIT_FAILED", "message" to "LMS initialization failed"))
        }

        val csrfToken = cookies["csrftoken"]
        if (csrfToken == null) {
            logger.warn("LMS did not return csrftoken, cookies: ${cookies.keys}")
            throw LmsException(mapOf("error" to "NO_CSRF_TOKEN", "message" to "LMS did not return CSRF token"))
        }
        logger.debug("Got CSRF token: ${csrfToken.take(10)}...")

        logger.info("Step 2: Posting login credentials...")
        val formData = LinkedMultiValueMap<String, String>().apply {
            add("email_or_username", userEmail)
            add("password", password)
        }

        data class LoginResult(
            val success: Boolean,
            val body: String?,
            val errorCode: String?,
            val errorMessage: String?,
        )

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

                if (response.statusCode.isError) {
                    val json = objectMapper.readTree(body)
                    val errorCode = json.path("error_code").asText("http_error")
                    val errorMessage = json.path("value").asText("LMS HTTP error: ${response.statusCode}")
                    LoginResult(false, body, errorCode, errorMessage)
                } else {
                    LoginResult(true, body, null, null)
                }
            }

        if (!loginResponse.success) {
            logger.warn("LMS login failed: ${loginResponse.errorMessage}")
            throw LmsException(
                mapOf(
                    "error" to (loginResponse.errorCode ?: "LMS_LOGIN_FAILED"),
                    "message" to (loginResponse.errorMessage ?: "LMS login failed"),
                    "details" to (loginResponse.body ?: ""),
                ),
            )
        }

        val loginJson = objectMapper.readTree(loginResponse.body)
        logger.info("LMS login response JSON: $loginJson")

        if (!loginJson.path("success").asBoolean(false)) {
            val errorCode = loginJson.path("error_code").asText("incorrect_credentials")
            val message = loginJson.path("value").asText("Authentication failed")
            logger.warn("LMS login returned failure: $message")
            throw LmsException(mapOf("error" to errorCode, "message" to message))
        }

        val refreshedCsrfToken = cookies["csrftoken"]
        if (refreshedCsrfToken == null) {
            logger.warn("LMS did not return csrftoken after login")
            throw LmsException(
                mapOf(
                    "error" to "NO_CSRF_AFTER_LOGIN",
                    "message" to "LMS did not return CSRF token after login",
                ),
            )
        }
        val expiresAt = extractExpiry(cookies["edx-jwt-cookie-header-payload"])
            ?: throw LmsException(
                mapOf(
                    "error" to "NO_EXPIRES_AT_AFTER_LOGIN",
                    "message" to "LMS did not return expiresAt after login",
                ),
            )


        logger.info("LMS authentication SUCCESS for user: $userEmail")
        return EdxAuthenticatedSession(
            cookieHeader = buildCookieHeader(cookies),
            csrfToken = refreshedCsrfToken,
            expiresAt = expiresAt,
        )
    }

    fun getUserEmail(session: EdxAuthenticatedSession, username: String): String? {
        logger.info("Fetching email from LMS for user: $username")

        return try {
            val response = authClient.get()
                .uri("/user/v1/accounts/{username}", username)
                .header(HttpHeaders.COOKIE, session.cookieHeader)
                .header("X-CSRFToken", session.csrfToken)
                .exchange { _, resp ->
                    if (resp.statusCode.isError) {
                        val body = resp.body.readAllBytes().toString(StandardCharsets.UTF_8)
                        logger.warn("Failed to fetch user email: ${resp.statusCode}, body: $body")
                        null
                    } else {
                        resp.body.readAllBytes().toString(StandardCharsets.UTF_8)
                    }
                }

            response?.let {
                val json = objectMapper.readTree(it)
                val email = json.path("email").asText(null)
                logger.info("Got email for $username: $email")
                email
            }
        } catch (e: Exception) {
            logger.error("Failed to get user email: ${e.message}", e)
            null
        }
    }
}
