package soaringLizards.blps.advice

import org.springframework.dao.DataIntegrityViolationException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.validation.FieldError
import org.springframework.validation.ObjectError
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.client.RestClientResponseException
import soaringLizards.blps.exception.LmsException
import soaringLizards.blps.exception.ResourceAlreadyExistsException
import soaringLizards.blps.exception.ResourceNotFoundException
import java.time.LocalDateTime
import java.util.function.Consumer

@RestControllerAdvice
class GlobalExceptionHandler {
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
    fun handleBadCredentials(ex: BadCredentialsException?) =
        ResponseEntity.status(HttpStatus.FORBIDDEN).body(
            hashMapOf(
                "timestamp" to LocalDateTime.now(),
                "status" to HttpStatus.FORBIDDEN.value(),
                "error" to "Forbidden",
                "message" to "Invalid login or password",
            ),
        )

    @ExceptionHandler(AccessDeniedException::class)
    fun handleAccessDenied(ex: AccessDeniedException?) =
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

    @ExceptionHandler(DataIntegrityViolationException::class)
    fun handleDataIntegrityViolation(ex: DataIntegrityViolationException) =
        ResponseEntity.status(HttpStatus.CONFLICT).body(
            hashMapOf(
                "timestamp" to LocalDateTime.now(),
                "status" to HttpStatus.CONFLICT.value(),
                "error" to "Conflict",
                "message" to "Data integrity violation",
                "details" to (ex.getMostSpecificCause().message ?: ""),
            ),
        )

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
        ex.printStackTrace()
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
            hashMapOf(
                "timestamp" to LocalDateTime.now(),
                "status" to HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "error" to "Internal Server Error",
                "message" to "An unexpected error occurred",
                "details" to (ex.message ?: ""),
            ),
        )
    }
}
