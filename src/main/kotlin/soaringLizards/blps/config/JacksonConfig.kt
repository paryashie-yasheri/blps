package soaringLizards.blps.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Lazy
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper

@Configuration
class JacksonConfig {
    @Bean
    @Lazy
    fun objectMapper(): ObjectMapper = jacksonObjectMapper()
}
