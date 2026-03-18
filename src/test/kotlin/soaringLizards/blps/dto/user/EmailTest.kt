package soaringLizards.blps.dto.user

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFalse

class EmailTest {

    @Test
    fun `should create valid email`() {
        val email = Email("test@example.com")
        assertEquals("test@example.com", email.value)
    }

    @Test
    fun `should create email with plus sign`() {
        val email = Email("user+tag@example.com")
        assertEquals("user+tag@example.com", email.value)
    }

    @Test
    fun `should create email with dots`() {
        val email = Email("first.last@example.com")
        assertEquals("first.last@example.com", email.value)
    }

    @Test
    fun `should create email with underscores`() {
        val email = Email("user_name@example.com")
        assertEquals("user_name@example.com", email.value)
    }

    @Test
    fun `should create email with hyphens`() {
        val email = Email("user-name@example.com")
        assertEquals("user-name@example.com", email.value)
    }

    @Test
    fun `should create email with subdomain`() {
        val email = Email("user@mail.example.com")
        assertEquals("user@mail.example.com", email.value)
    }

    @ParameterizedTest
    @ValueSource(strings = [
        "test@example.com",
        "user.name@example.com",
        "user+tag@example.com",
        "user_name@example.com",
        "user-name@example.com",
        "123@example.com",
        "user@example.co.uk",
        "user@subdomain.example.com"
    ])
    fun `should accept valid email formats`(emailString: String) {
        assertDoesNotThrow { Email(emailString) }
    }

    @ParameterizedTest
    @ValueSource(strings = [
        "",
        "invalid",
        "@example.com",
        "user@",
        "user@.com",
        "user@example",
        "user name@example.com",
        "user@example .com",
        "user@@example.com"
    ])
    fun `should reject invalid email formats`(emailString: String) {
        assertThrows<IllegalArgumentException> { Email(emailString) }
    }

    @Test
    fun `should throw exception with descriptive message for invalid email`() {
        val exception = assertThrows<IllegalArgumentException> {
            Email("invalid-email")
        }
        assertTrue(exception.message!!.contains("Invalid email format"))
        assertTrue(exception.message!!.contains("invalid-email"))
    }

    @Test
    fun `of factory method should return success for valid email`() {
        val result = Email.of("test@example.com")
        assertTrue(result.isSuccess)
        assertEquals("test@example.com", result.getOrNull()?.value)
    }

    @Test
    fun `of factory method should return failure for invalid email`() {
        val result = Email.of("invalid-email")
        assertTrue(result.isFailure)
    }

    @Test
    fun `emails with same value should be equal`() {
        val email1 = Email("test@example.com")
        val email2 = Email("test@example.com")
        assertEquals(email1.value, email2.value)
    }

    @Test
    fun `should create email with numbers in local part`() {
        val email = Email("user123@example.com")
        assertEquals("user123@example.com", email.value)
    }

    @Test
    fun `should create email with numbers in domain`() {
        val email = Email("user@example123.com")
        assertEquals("user@example123.com", email.value)
    }

    @Test
    fun `should handle long tld`() {
        val email = Email("user@example.technology")
        assertEquals("user@example.technology", email.value)
    }
}