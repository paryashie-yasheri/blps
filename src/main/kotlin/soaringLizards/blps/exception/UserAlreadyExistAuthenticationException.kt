package soaringLizards.blps.exception

import org.springframework.security.core.AuthenticationException

class UserAlreadyExistAuthenticationException(msg: String?) : AuthenticationException(msg)
