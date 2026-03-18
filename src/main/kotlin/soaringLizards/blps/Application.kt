package soaringLizards.blps

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication
import soaringLizards.blps.config.AuthorizationServerProperties

@SpringBootApplication
@EnableConfigurationProperties(AuthorizationServerProperties::class)
class Application

fun main(args: Array<String>) {
    runApplication<Application>(*args)
}
