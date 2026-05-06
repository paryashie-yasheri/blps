package soaringLizards.blps.advice

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import java.sql.SQLException

class GlobalExceptionHandlerTest {
    private val handler = GlobalExceptionHandler()

    @Test
    fun `database failures return service unavailable without internal details`() {
        val response = handler.handleDatabaseUnavailable(
            SQLException("connection refused for jdbc:postgresql://localhost:5434/studs_rewards"),
        )

        assertThat(response.statusCode).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE)
        assertThat(response.body).containsEntry("status", HttpStatus.SERVICE_UNAVAILABLE.value())
        assertThat(response.body).containsEntry("error", "Service Unavailable")
        assertThat(response.body).containsEntry("message", "Required database is unavailable")
        assertThat(response.body).doesNotContainKey("details")
    }

    @Test
    fun `generic failures do not leak exception messages`() {
        val response = handler.handleGenericException(
            IllegalStateException("internal host=localhost port=5432"),
        )

        assertThat(response.statusCode).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR)
        assertThat(response.body).containsEntry("message", "An unexpected error occurred")
        assertThat(response.body).doesNotContainKey("details")
    }

    @Test
    fun `wrapped database failures return service unavailable`() {
        val response = handler.handleRuntimeException(
            IllegalStateException(
                "transaction failed",
                SQLException("connection refused for jdbc:postgresql://localhost:5434/studs_journal"),
            ),
        )

        assertThat(response.statusCode).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE)
        assertThat(response.body).containsEntry("status", HttpStatus.SERVICE_UNAVAILABLE.value())
        assertThat(response.body).containsEntry("message", "Required database is unavailable")
        assertThat(response.body).doesNotContainKey("details")
    }
}
