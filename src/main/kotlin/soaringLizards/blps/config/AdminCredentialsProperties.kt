package soaringLizards.blps.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "spring.security.admin-credentials")
data class AdminCredentialsProperties(
    val username: String,
    val password: String,
    val email: String,
)