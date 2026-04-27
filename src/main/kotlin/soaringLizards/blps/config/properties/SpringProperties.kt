package soaringLizards.blps.config.properties

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "spring.env.config")
data class SpringProperties(
    val port: Int,
    val host: String,
)
