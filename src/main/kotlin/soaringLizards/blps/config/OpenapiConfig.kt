package soaringLizards.blps.config

import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.security.SecurityRequirement
import io.swagger.v3.oas.models.security.SecurityScheme
import io.swagger.v3.oas.models.servers.Server
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class OpenApiConfig {
    @Bean
    fun customOpenAPI(): OpenAPI = OpenAPI()
        .info(Info().title("BLPS Api").version("1.0"))
        .addSecurityItem(SecurityRequirement().addList("basicAuth"))
        .components(
            Components()
                .addSecuritySchemes(
                    "basicAuth",
                    SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("basic"),
                ),
        )
        .servers(
            listOf(
                Server().url("http://localhost:1234").description("Local"), //TODO убрать хардкод, доставать из .env порт и хост
            ),
        )
}
