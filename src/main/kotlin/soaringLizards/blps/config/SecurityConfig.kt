package soaringLizards.blps.config

import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.Customizer.withDefaults
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.core.userdetails.User
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.SecurityFilterChain
import soaringLizards.blps.repository.UserRepository

@Configuration
@EnableWebSecurity
class SecurityConfig {
    @Bean
    fun passwordEncoder(): PasswordEncoder {
        return BCryptPasswordEncoder()
    }

    companion object {
        private val logger = LoggerFactory.getLogger(SecurityConfig::class.java)
    }

    @Bean
    fun filterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .authorizeHttpRequests { auth ->
                auth
                    .requestMatchers("/", "/api/auth/**", "/error", "/public/**").permitAll()
                    .anyRequest().authenticated()
            }
            .formLogin(withDefaults())
            .csrf { csrf ->
                csrf.ignoringRequestMatchers("/api/**", "/login") // Dev only!
            }

        return http.build()
    }

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