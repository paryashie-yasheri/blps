package soaringLizards.blps.error

import org.springframework.http.HttpStatus

open class ApiException(
    val status: HttpStatus,
    override val message: String,
) : RuntimeException(message)

class NotFoundException(message: String = "Not found") : ApiException(HttpStatus.NOT_FOUND, message)
class ConflictException(message: String = "Conflict") : ApiException(HttpStatus.CONFLICT, message)
class BadRequestException(message: String = "Bad request") : ApiException(HttpStatus.BAD_REQUEST, message)
class ForbiddenException(message: String = "Forbidden") : ApiException(HttpStatus.FORBIDDEN, message)
class UnauthorizedException(message: String = "Unauthorized") : ApiException(HttpStatus.UNAUTHORIZED, message)