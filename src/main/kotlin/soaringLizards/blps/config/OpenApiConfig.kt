package soaringLizards.blps.config

import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.security.SecurityRequirement
import io.swagger.v3.oas.models.security.SecurityScheme
import io.swagger.v3.oas.models.servers.Server
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springdoc.core.customizers.OpenApiCustomizer

@Configuration
class OpenApiConfig {
    @Bean
    fun customOpenAPI(): OpenAPI = OpenAPI()
        .info(Info().title("BLPS Api").version("1.0"))
        .components(
            Components()
                .addSecuritySchemes(
                    "basicAuth",
                    SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("basic"),
                )
                .addSecuritySchemes(
                    "bearerAuth",
                    SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT"),
                ),
        )
        .servers(
            listOf(
                Server().url("http://localhost:1234").description("Local"), //TODO убрать хардкод, доставать из .env порт и хост
            ),
        )

    @Bean
    fun securityRequirementsCustomizer(): OpenApiCustomizer = OpenApiCustomizer { openApi ->
        val bearerAuth = listOf(SecurityRequirement().addList("bearerAuth"))
        val noSecurity = emptyList<SecurityRequirement>()

        openApi.paths.orEmpty().forEach { (path, pathItem) ->
            pathItem.readOperationsMap().forEach { (httpMethod, operation) ->
                operation.security = when {
                    path == "/api/auth/staff/login" || path == "/api/auth/openedx_user/login" -> noSecurity
                    path == "/api/test/roles/public" -> noSecurity
                    path.startsWith("/api/student") -> bearerAuth
                    path.startsWith("/api/edx") -> bearerAuth
                    path.startsWith("/api/user-balance") -> bearerAuth
                    path == "/api/auth/me" -> bearerAuth
                    path == "/api/auth/register-admin" -> bearerAuth
                    path == "/api/auth/register-staff" -> bearerAuth
                    path.startsWith("/api/achievements") -> bearerAuth
                    path == "/api/img/s3" && httpMethod.name == "POST" -> bearerAuth
                    path.startsWith("/api/img/s3/") && httpMethod.name == "DELETE" -> bearerAuth
                    path == "/api/test/roles/user" -> bearerAuth
                    path == "/api/test/roles/staff" -> bearerAuth
                    path == "/api/test/roles/admin" -> bearerAuth
                    path == "/api/test/roles/user-or-staff" -> bearerAuth
                    path == "/api/test/roles/staff-or-admin" -> bearerAuth
                    path == "/api/test/roles/any-authenticated" -> bearerAuth
                    else -> operation.security ?: noSecurity
                }
            }
        }
    }
}
