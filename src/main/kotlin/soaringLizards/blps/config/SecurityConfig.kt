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
import org.springframework.security.core.userdetails.User
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint
import soaringLizards.blps.repository.UserRepository

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@Order(1)
class SecurityConfig {

    companion object {
        private val logger = LoggerFactory.getLogger(SecurityConfig::class.java)
    }

    @Bean
    fun passwordEncoder(): PasswordEncoder {
        return BCryptPasswordEncoder()
    }

    @Bean
    fun filterChain(http: HttpSecurity) =
        http
            .authorizeHttpRequests { auth ->
                auth
                    .requestMatchers("/", "/api/auth/**", "/error", "/public/**", "/api/test/roles/public").permitAll()
                    .anyRequest().authenticated()
            }
            .exceptionHandling { exceptions: ExceptionHandlingConfigurer<HttpSecurity?>? ->
                exceptions!!.authenticationEntryPoint(
                    LoginUrlAuthenticationEntryPoint("/login")
                )
            }
            .formLogin(withDefaults())
            .csrf { it.disable() }
            .build()

    @Bean
    fun userDetailsService(userRepository: UserRepository): UserDetailsService {
        return UserDetailsService { username: String ->
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
}
