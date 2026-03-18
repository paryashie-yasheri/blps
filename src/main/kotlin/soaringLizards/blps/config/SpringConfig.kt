package soaringLizards.blps.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "spring.env.config")
data class SpringConfig(
    val port: Int,
    val host: String,
)
