package soaringLizards.blps.exception

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
import java.time.LocalDateTime
import java.util.function.Consumer

@RestControllerAdvice
class GlobalExceptionHandler {
    @ExceptionHandler(ResourceNotFoundException::class)
    fun handleNotFound(ex: ResourceNotFoundException): ResponseEntity<MutableMap<String, Any>> {
        val response: MutableMap<String, Any> = hashMapOf(
            "timestamp" to LocalDateTime.now(),
            "status" to HttpStatus.NOT_FOUND.value(),
            "error" to "Not Found",
            "message" to (ex.message ?: ""),
        )
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response)
    }

    @ExceptionHandler(ResourceAlreadyExistsException::class)
    fun handleAlreadyExists(ex: ResourceAlreadyExistsException): ResponseEntity<MutableMap<String, Any>> {
        val response: MutableMap<String, Any> = hashMapOf(
            "timestamp" to LocalDateTime.now(),
            "status" to HttpStatus.CONFLICT.value(),
            "error" to "Conflict",
            "message" to (ex.message ?: ""),
        )
        return ResponseEntity.status(HttpStatus.CONFLICT).body(response)
    }

    @ExceptionHandler(BadCredentialsException::class)
    fun handleBadCredentials(ex: BadCredentialsException?): ResponseEntity<MutableMap<String, Any>> {
        val response: MutableMap<String, Any> = hashMapOf(
            "timestamp" to LocalDateTime.now(),
            "status" to HttpStatus.FORBIDDEN.value(),
            "error" to "Forbidden",
            "message" to "Invalid login or password",
        )
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response)
    }

    @ExceptionHandler(AccessDeniedException::class)
    fun handleAccessDenied(ex: AccessDeniedException?): ResponseEntity<MutableMap<String, Any>> {
        val response: MutableMap<String, Any> = hashMapOf(
            "timestamp" to LocalDateTime.now(),
            "status" to HttpStatus.FORBIDDEN.value(),
            "error" to "Forbidden",
            "message" to "Access denied",
        )
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response)
    }

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidation(ex: MethodArgumentNotValidException): ResponseEntity<MutableMap<String, Any>> {
        val errors: MutableMap<String?, String?> = HashMap()
        ex.bindingResult.allErrors.forEach(Consumer { error: ObjectError? ->
            val fieldName = (error as FieldError).field
            val message = error.defaultMessage
            errors[fieldName] = message
        })

        val response: MutableMap<String, Any> = hashMapOf(
            "timestamp" to LocalDateTime.now(),
            "status" to HttpStatus.BAD_REQUEST.value(),
            "error" to "Validation Error",
            "message" to "Validation failed for request",
            "errors" to errors,
        )
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response)
    }

    @ExceptionHandler(DataIntegrityViolationException::class)
    fun handleDataIntegrityViolation(ex: DataIntegrityViolationException): ResponseEntity<MutableMap<String, Any>> {
        val response: MutableMap<String, Any> = hashMapOf(
            "timestamp" to LocalDateTime.now(),
            "status" to HttpStatus.CONFLICT.value(),
            "error" to "Conflict",
            "message" to "Data integrity violation",
            "details" to (ex.getMostSpecificCause().message ?: ""),
        )
        return ResponseEntity.status(HttpStatus.CONFLICT).body(response)
    }

    @ExceptionHandler(Exception::class)
    fun handleGenericException(ex: Exception): ResponseEntity<MutableMap<String, Any>> {
        val response: MutableMap<String, Any> = hashMapOf(
            "timestamp" to LocalDateTime.now(),
            "status" to HttpStatus.INTERNAL_SERVER_ERROR.value(),
            "error" to "Internal Server Error",
            "message" to "An unexpected error occurred",
            "details" to (ex.message ?: ""),
        )
        ex.printStackTrace()
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response)
    }
}