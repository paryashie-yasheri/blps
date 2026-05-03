package soaringLizards.blps.advice

import jakarta.transaction.HeuristicMixedException
import jakarta.transaction.HeuristicRollbackException
import jakarta.transaction.RollbackException
import jakarta.transaction.SystemException
import org.slf4j.LoggerFactory
import org.springframework.dao.DataAccessException
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.validation.FieldError
import org.springframework.validation.ObjectError
import org.springframework.transaction.TransactionException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.client.RestClientResponseException
import soaringLizards.blps.exception.BusinessLogicException
import soaringLizards.blps.exception.LmsException
import org.springframework.http.converter.HttpMessageNotReadableException
import soaringLizards.blps.exception.ResourceAlreadyExistsException
import soaringLizards.blps.exception.ResourceNotFoundException
import java.sql.SQLException
import java.time.LocalDateTime
import java.util.function.Consumer

@RestControllerAdvice
class GlobalExceptionHandler {
    private val logger = LoggerFactory.getLogger(javaClass)

    @ExceptionHandler(ResourceNotFoundException::class)
    fun handleNotFound(ex: ResourceNotFoundException) =
        ResponseEntity.status(HttpStatus.NOT_FOUND).body(
            hashMapOf(
                "timestamp" to LocalDateTime.now(),
                "status" to HttpStatus.NOT_FOUND.value(),
                "error" to "Not Found",
                "message" to (ex.message ?: ""),
            ),
        )

    @ExceptionHandler(ResourceAlreadyExistsException::class)
    fun handleAlreadyExists(ex: ResourceAlreadyExistsException) =
        ResponseEntity.status(HttpStatus.CONFLICT).body(
            hashMapOf(
                "timestamp" to LocalDateTime.now(),
                "status" to HttpStatus.CONFLICT.value(),
                "error" to "Conflict",
                "message" to (ex.message ?: ""),
            ),
        )

    @ExceptionHandler(BadCredentialsException::class)
    fun handleBadCredentials() =
        ResponseEntity.status(HttpStatus.FORBIDDEN).body(
            hashMapOf(
                "timestamp" to LocalDateTime.now(),
                "status" to HttpStatus.FORBIDDEN.value(),
                "error" to "Forbidden",
                "message" to "Invalid login or password",
            ),
        )

    @ExceptionHandler(AccessDeniedException::class)
    fun handleAccessDenied() =
        ResponseEntity
            .status(HttpStatus.FORBIDDEN)
            .body(
                hashMapOf(
                    "timestamp" to LocalDateTime.now(),
                    "status" to HttpStatus.FORBIDDEN.value(),
                    "error" to "Forbidden",
                    "message" to "Access denied",
                ),
            )

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidation(ex: MethodArgumentNotValidException): ResponseEntity<MutableMap<String, Any>> {
        val errors: MutableMap<String?, String?> = HashMap()
        ex.bindingResult.allErrors.forEach(
            Consumer { error: ObjectError? ->
                val fieldName = (error as FieldError).field
                val message = error.defaultMessage
                errors[fieldName] = message
            },
        )

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
            hashMapOf(
                "timestamp" to LocalDateTime.now(),
                "status" to HttpStatus.BAD_REQUEST.value(),
                "error" to "Validation Error",
                "message" to "Validation failed for request",
                "errors" to errors,
            ),
        )
    }

    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handleUnreadableMessage(ex: HttpMessageNotReadableException) =
        ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
            hashMapOf(
                "timestamp" to LocalDateTime.now(),
                "status" to HttpStatus.BAD_REQUEST.value(),
                "error" to "Bad Request",
                "message" to "Request body is invalid",
                "details" to (ex.mostSpecificCause.message ?: ex.message ?: ""),
            ),
        )

    @ExceptionHandler(BusinessLogicException::class)
    fun handleBusinessLogic(ex: BusinessLogicException) =
        ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
            hashMapOf(
                "timestamp" to LocalDateTime.now(),
                "status" to HttpStatus.BAD_REQUEST.value(),
                "error" to "Bad Request",
                "message" to (ex.message ?: ""),
            ),
        )

    @ExceptionHandler(DataIntegrityViolationException::class)
    fun handleDataIntegrityViolation(ex: DataIntegrityViolationException): ResponseEntity<MutableMap<String, Any>> {
        logger.warn("data_integrity_violation", ex)
        return errorResponse(HttpStatus.CONFLICT, "Conflict", "Data integrity violation")
    }

    @ExceptionHandler(
        DataAccessException::class,
        org.jooq.exception.DataAccessException::class,
        SQLException::class,
        TransactionException::class,
        SystemException::class,
        RollbackException::class,
        HeuristicMixedException::class,
        HeuristicRollbackException::class,
    )
    fun handleDatabaseUnavailable(ex: Exception): ResponseEntity<MutableMap<String, Any>> {
        logger.error("database_or_transaction_unavailable", ex)
        return errorResponse(
            HttpStatus.SERVICE_UNAVAILABLE,
            "Service Unavailable",
            "Required database is unavailable",
        )
    }

    @ExceptionHandler(RestClientResponseException::class)
    fun handleRestClientResponse(ex: RestClientResponseException) =
        ResponseEntity.status(ex.statusCode).body(
            hashMapOf(
                "timestamp" to LocalDateTime.now(),
                "status" to ex.statusCode.value(),
                "message" to (ex.statusText.ifBlank { "Downstream API Error" }),
                "details" to ex.responseBodyAsString,
            ),
        )


    @ExceptionHandler(LmsException::class)
    fun handleLmsResponse(ex: LmsException) =
        ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
            hashMapOf(
                "timestamp" to LocalDateTime.now(),
                "status" to HttpStatus.BAD_REQUEST.value(),
                "error" to (ex.errors["error"] ?: "lms_error"),
                "message" to (ex.errors["message"] ?: "Open edX request failed"),
                "details" to (ex.errors["details"] ?: ""),
            ),
        )

    @ExceptionHandler(Exception::class)
    fun handleGenericException(ex: Exception): ResponseEntity<MutableMap<String, Any>> {
        logger.error("unhandled_exception", ex)
        return errorResponse(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "Internal Server Error",
            "An unexpected error occurred",
        )
    }

    private fun errorResponse(
        status: HttpStatus,
        error: String,
        message: String,
    ): ResponseEntity<MutableMap<String, Any>> =
        ResponseEntity.status(status).body(
            hashMapOf(
                "timestamp" to LocalDateTime.now(),
                "status" to status.value(),
                "error" to error,
                "message" to message,
            ),
        )
}
