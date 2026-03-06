package soaringLizards.blps.auth

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import net.logstash.logback.argument.StructuredArguments.kv
import org.slf4j.LoggerFactory
import org.springframework.http.HttpHeaders
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.filter.OncePerRequestFilter

class JwtTokenFilter(private val jwtService: JwtService) : OncePerRequestFilter() {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        extractToken(request)?.let { token ->
            val authentication = jwtService.parseAuthentication(token)
            if (authentication != null) {
                SecurityContextHolder.getContext().authentication = authentication
                log.debug(
                    "jwt_authenticated",
                    kv("userId", authentication.name),
                    kv("authorities", authentication.authorities.map { it.authority })
                )
            } else {
                log.debug(
                    "jwt_authentication_failed",
                    kv("path", request.requestURI)
                )
            }
        }
        filterChain.doFilter(request, response)
    }

    private fun extractToken(request: HttpServletRequest): String? {
        val header = request.getHeader(HttpHeaders.AUTHORIZATION) ?: return null
        return header.takeIf { it.startsWith("Bearer ", ignoreCase = true) }
            ?.substringAfter("Bearer ")
            ?.trim()
            ?.takeIf { it.isNotBlank() }
    }
}