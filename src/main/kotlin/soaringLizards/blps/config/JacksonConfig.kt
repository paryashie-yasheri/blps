package soaringLizards.blps.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Lazy
import tools.jackson.databind.ObjectMapper
import tools.jackson.module.kotlin.jacksonObjectMapper

@Configuration
class JacksonConfig {
    @Bean
    @Lazy
    fun objectMapper(): ObjectMapper = jacksonObjectMapper()
}
