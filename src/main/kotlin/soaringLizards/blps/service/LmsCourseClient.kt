package soaringLizards.blps.service

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate
import org.springframework.http.*
import org.springframework.security.oauth2.server.resource.authentication.BearerTokenAuthentication

@Service
class LmsCourseClient(
    private val restTemplate: RestTemplate,
    
    @Value("\${edx.api.base-url:http://local.openedx.io/api}")
    private val edxApiBaseUrl: String,
    
    @Value("\${edx.api.authorization:}")
    private val edxApiAuthorization: String,
) {
    
    fun getCompletedCourses(authentication: BearerTokenAuthentication): List<CourseDTO> {
        val tutorJwt = authentication.token.tokenValue
        
        return try {
            val headers = HttpHeaders()
            headers.contentType = MediaType.APPLICATION_JSON
            headers.setBearerAuth(tutorJwt)
            
            val entity = HttpEntity<String>(headers)
            val response = restTemplate.exchange(
                "$edxApiBaseUrl/v1/courses/completed/",
                HttpMethod.GET,
                entity,
                List::class.java
            )
            
            // Convert LMS response to internal DTOs
            response.body?.let { courses ->
                @Suppress("UNCHECKED_CAST")
                courses.map { course ->
                    CourseDTO(
                        id = (course as Map<String, Any>)["id"]?.toString() ?: "",
                        name = (course as Map<String, Any>)["name"]?.toString() ?: "",
                        description = (course as Map<String, Any>)["description"]?.toString() ?: "",
                        completedAt = (course as Map<String, Any>)["completed_at"]?.toString() ?: ""
                    )
                }
            } ?: emptyList()
            
        } catch (e: org.springframework.web.client.HttpClientErrorException) {
            when (e.statusCode) {
                HttpStatus.UNAUTHORIZED -> throw org.springframework.security.access.AccessDeniedException("LMS authentication failed")
                HttpStatus.TOO_MANY_REQUESTS -> throw RuntimeException("LMS rate limit exceeded, please try again later")
                else -> throw RuntimeException("LMS error: ${e.message}")
            }
        } catch (e: Exception) {
            throw RuntimeException("Failed to fetch courses from LMS: ${e.message}")
        }
    }
}

data class CourseDTO(
    val id: String,
    val name: String,
    val description: String,
    val completedAt: String,
)