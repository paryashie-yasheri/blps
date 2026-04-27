package soaringLizards.blps

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication
import soaringLizards.blps.config.properties.AdminCredentialsProperties
import soaringLizards.blps.config.properties.AuthorizationServerProperties
import soaringLizards.blps.config.properties.S3Properties

@SpringBootApplication
@EnableConfigurationProperties(
    AuthorizationServerProperties::class,
    AdminCredentialsProperties::class,
    S3Properties::class,
)
class Application

fun main(args: Array<String>) {
    runApplication<Application>(*args)
}
