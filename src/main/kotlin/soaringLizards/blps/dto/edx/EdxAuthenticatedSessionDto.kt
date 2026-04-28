package soaringLizards.blps.dto.edx

import java.time.Instant

data class EdxAuthenticatedSession(
    val cookieHeader: String,
    val csrfToken: String,
    val expiresAt: Instant,
)
