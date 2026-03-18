package soaringLizards.blps.config

import org.springframework.boot.context.properties.ConfigurationProperties
import soaringLizards.blps.dto.user.Email

@ConfigurationProperties(prefix = "spring.security.admin-credentials")
data class AdminCredentialsProperties(
    val username: String,
    val password: String,
    val email: Email,
)
