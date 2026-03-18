package soaringLizards.blps.config

import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.jwt.Jwt
import org.openapitools.client.infrastructure.ApiClient
import soaringLizards.blps.edx.api.CoursesApi
import java.util.concurrent.TimeUnit

@Configuration
class EdxApiConfig {

    @Value("\${edx.api.base-url:http://local.openedx.io}")
    private lateinit var baseUrl: String

    @Bean
    fun okHttpClientBuilder(): OkHttpClient.Builder {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val authInterceptor = Interceptor { chain ->
            val originalRequest = chain.request()
            val token = extractToken()
            
            val newRequest = if (token != null) {
                originalRequest.newBuilder()
                    .addHeader("Authorization", "Bearer $token")
                    .build()
            } else {
                originalRequest
            }
            
            chain.proceed(newRequest)
        }

        return OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(logging)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
    }

    @Bean
    fun apiClient(okHttpClientBuilder: OkHttpClient.Builder): ApiClient {
        return ApiClient(
            baseUrl = baseUrl.trimEnd('/') + "/",
            okHttpClientBuilder = okHttpClientBuilder
        )
    }

    @Bean
    fun coursesApi(apiClient: ApiClient): CoursesApi {
        return apiClient.createService(CoursesApi::class.java)
    }

    private fun extractToken(): String? {
        val authentication: Authentication? = SecurityContextHolder.getContext().authentication
        return when (val principal = authentication?.principal) {
            is Jwt -> principal.tokenValue
            is String -> principal
            else -> null
        }
    }
}
