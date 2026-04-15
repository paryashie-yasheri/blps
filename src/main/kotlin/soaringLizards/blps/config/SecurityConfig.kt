package soaringLizards.blps.config

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.annotation.Order
import org.springframework.security.config.Customizer.withDefaults
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.configurers.ExceptionHandlingConfigurer
import org.springframework.security.core.userdetails.User
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint
import soaringLizards.blps.repository.UserRepository
import javax.crypto.spec.SecretKeySpec

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
class SecurityConfig(
    private val userRepository: UserRepository,
    @Value("\${app.jwt.staff.secret}") private val staffJwtSecret: String,
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
                auth.anyRequest().hasRole("STAFF")
            }
            .oauth2ResourceServer { oauth2 ->
                oauth2.jwt { jwt ->
                    jwt.decoder(staffJwtDecoder())
                    jwt.jwtAuthenticationConverter(staffJwtAuthenticationConverter())
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
                    ).permitAll()
                    .anyRequest().authenticated()
            }
            .formLogin(withDefaults())
            .httpBasic(withDefaults())
            .oauth2ResourceServer { oauth2 ->
                oauth2.jwt { jwt ->
                    jwt.decoder(staffJwtDecoder())  // Use staff secret for all JWT
                }
            }
            .csrf { it.disable() }
            .build()!!

    @Bean
    fun staffJwtDecoder(): JwtDecoder {
        val secretKey = SecretKeySpec(staffJwtSecret.toByteArray(), "HmacSHA256")
        return NimbusJwtDecoder.withSecretKey(secretKey).build()
    }

    @Bean
    fun staffJwtAuthenticationConverter(): JwtAuthenticationConverter {
        val converter = JwtAuthenticationConverter()
        converter.setJwtGrantedAuthoritiesConverter(staffJwtGrantedAuthoritiesConverter())
        return converter
    }

    @Bean
    fun staffJwtGrantedAuthoritiesConverter(): JwtGrantedAuthoritiesConverter {
        val converter = JwtGrantedAuthoritiesConverter()
        converter.setAuthorityPrefix("ROLE_")
        return converter
    }

    @Bean
    fun userDetailsService() =
        UserDetailsService { username: String ->
            logger.info("Loading user: $username")
            val user = userRepository.findByUsername(username)
                ?: throw UsernameNotFoundException("User not found: $username")

            logger.info("User found, roles: ${user.role}")

            User
                .withUsername(user.username!!)
                .password(user.passwordHash!!)
                .roles(user.role!!)
                .build()
        }
}