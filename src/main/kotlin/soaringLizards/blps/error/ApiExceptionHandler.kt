package soaringLizards.blps.error

import io.swagger.v3.oas.annotations.media.Schema
import net.logstash.logback.argument.StructuredArguments.kv
import org.slf4j.LoggerFactory
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.context.request.WebRequest
import java.sql.SQLException

@RestControllerAdvice
class ApiExceptionHandler {

    private val logger = LoggerFactory.getLogger(javaClass)

    @ExceptionHandler(ApiException::class)
    fun handleApiException(ex: ApiException, request: WebRequest): ResponseEntity<ApiError> {
        logger.warn(
            "api_exception",
            kv("status", ex.status.value()),
            kv("error", ex.status.reasonPhrase),
            kv("message", ex.message),
            kv("path", request.getDescription(false))
        )
        val body = ApiError(
            status = ex.status.value(),
            error = ex.status.reasonPhrase,
            message = ex.message
        )
        return ResponseEntity.status(ex.status).body(body)
    }

    @ExceptionHandler(DataIntegrityViolationException::class)
    fun handleDataIntegrity(ex: DataIntegrityViolationException, request: WebRequest): ResponseEntity<ApiError> =
        handleDatabaseException(ex, request)

    @ExceptionHandler(org.jooq.exception.DataAccessException::class)
    fun handleJooqDataAccess(
        ex: org.jooq.exception.DataAccessException,
        request: WebRequest,
    ): ResponseEntity<ApiError> =
        handleDatabaseException(ex, request)

    private fun handleDatabaseException(ex: Exception, request: WebRequest): ResponseEntity<ApiError> {
        val sql = findSqlException(ex)
        val sqlState = sql?.sqlState
        val rawMessage = sql?.message
        val message = extractDbMessage(rawMessage)
        val constraint = extractConstraintName(rawMessage)

        val status = when (sqlState) {
            "23505" -> HttpStatus.CONFLICT // unique violation
            "23503" -> HttpStatus.BAD_REQUEST // foreign key violation
            else -> HttpStatus.INTERNAL_SERVER_ERROR
        }

        val safeMessage: String = when {
            status == HttpStatus.INTERNAL_SERVER_ERROR -> "Internal server error"
            constraint == "uq_profiles_username" -> "Username already taken"
            message != null -> message
            else -> "Request violates database constraints"
        }

        logger.warn(
            "db_constraint_violation",
            kv("status", status.value()),
            kv("sqlState", sqlState),
            kv("constraint", constraint),
            kv("message", safeMessage),
            kv("path", request.getDescription(false)),
            ex
        )

        return ResponseEntity.status(status).body(
            ApiError(
                status = status.value(),
                error = status.reasonPhrase,
                message = safeMessage
            )
        )
    }

    private fun findSqlException(ex: Throwable): SQLException? {
        var cur: Throwable? = ex
        while (cur != null) {
            if (cur is SQLException) return cur
            cur = cur.cause
        }
        return null
    }

    private fun extractDbMessage(rawMessage: String?): String? {
        if (rawMessage.isNullOrBlank()) return null
        // Common PostgreSQL format: "ERROR: <msg>\n  Detail: ...\n  Where: ..."
        val idx = rawMessage.indexOf("ERROR:")
        return if (idx >= 0) {
            rawMessage.substring(idx + "ERROR:".length)
                .substringBefore('\n')
                .trim()
                .takeIf { it.isNotBlank() }
        } else {
            rawMessage.substringBefore('\n').trim().takeIf { it.isNotBlank() }
        }
    }

    private fun extractConstraintName(rawMessage: String?): String? {
        if (rawMessage.isNullOrBlank()) return null
        val regex = Regex("constraint\\s+\\\"([^\\\"]+)\\\"")
        return regex.find(rawMessage)?.groupValues?.getOrNull(1)
    }

    @ExceptionHandler(Exception::class)
    fun handleUnhandledException(ex: Exception, request: WebRequest): ResponseEntity<ApiError> {
        logger.error(
            "unhandled_exception",
            kv("exception", ex::class.qualifiedName),
            kv("message", ex.message),
            kv("path", request.getDescription(false)),
            ex
        )
        val status = HttpStatus.INTERNAL_SERVER_ERROR
        val body = ApiError(
            status = status.value(),
            error = status.reasonPhrase,
            message = "Internal server error"
        )
        return ResponseEntity.status(status).body(body)
    }

    @Schema(name = "ApiError", description = "Standard error response")
    data class ApiError(
        @field:Schema(example = "404")
        val status: Int,
        @field:Schema(example = "Not Found")
        val error: String,
        @field:Schema(example = "User not found")
        val message: String?,
    )
}