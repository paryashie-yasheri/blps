package soaringLizards.blps.config

import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.annotation.Order
import org.springframework.security.config.Customizer.withDefaults
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.configurers.ExceptionHandlingConfigurer
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.web.util.matcher.AntPathRequestMatcher
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint
import soaringLizards.blps.config.userDetails.JwtToAbstractAuthenticationTokenConverter

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
class SecurityConfig(
    private val keycloakJwtDecoder: KeycloakJwtDecoder,
) {

    companion object {
        private val logger = LoggerFactory.getLogger(SecurityConfig::class.java)
    }

    @Bean
    fun passwordEncoder() = BCryptPasswordEncoder()

    @Bean
    @Order(1)
    fun staffSecurityFilterChain(http: HttpSecurity): SecurityFilterChain {
        return http
            .securityMatcher("/api/staff/**")
            .authorizeHttpRequests { auth ->
                auth.requestMatchers("/api/staff/auth/**").permitAll()
                auth.anyRequest().hasAnyRole("STAFF", "ADMIN")
            }
            .oauth2ResourceServer { oauth2 ->
                oauth2.jwt { jwt ->
                    jwt.decoder(keycloakJwtDecoder)
                    jwt.jwtAuthenticationConverter(JwtToAbstractAuthenticationTokenConverter())
                }
            }
            .exceptionHandling { exceptions: ExceptionHandlingConfigurer<HttpSecurity?>? ->
                exceptions!!.authenticationEntryPoint(
                    LoginUrlAuthenticationEntryPoint("/login"),
                )
            }
            .csrf { it.disable() }
            .build()!!
    }

    @Bean
    @Order(2)
    fun camundaSecurityFilterChain(http: HttpSecurity): SecurityFilterChain {
        return http
            .securityMatcher(AntPathRequestMatcher("/camunda/**"))
            .authorizeHttpRequests { auth ->
                auth.anyRequest().permitAll()
            }
            .csrf { it.disable() }
            .headers { it.frameOptions { frame -> frame.sameOrigin() } }
            .build()!!
    }

    @Bean
    @Order(3)
    fun filterChain(http: HttpSecurity) =
        http
            .authorizeHttpRequests { auth ->
                auth
                    .requestMatchers(
                        "/",
                        "/api/auth/**",
                        "/login",
                        "/error",
                        "/public/**",
                        "/api/test/roles/public",
                        "/api-docs",
                        "/swagger-ui/**",
                        "/swagger-ui.html",
                        "/oauth2/**",
                        "/v3/api-docs/**",
                        "/actuator/**",
                        "/engine-rest/**",
                    ).permitAll()
                    .anyRequest().authenticated()
            }
            .oauth2ResourceServer { oauth2 ->
                oauth2.jwt { jwt ->
                    jwt.decoder(keycloakJwtDecoder)
                    jwt.jwtAuthenticationConverter(JwtToAbstractAuthenticationTokenConverter())

                }
            }
            .csrf { it.disable() }
            .build()!!
}
