package soaringLizards.blps.service

import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import soaringLizards.blps.client.EdxCoursesClient
import soaringLizards.blps.client.EdxEnrollmentClient
import soaringLizards.blps.client.EdxProgressClient
import soaringLizards.blps.config.userDetails.JwtUserDetails
import soaringLizards.blps.dto.edx.EdxAuthenticatedSession
import soaringLizards.blps.dto.edx.EdxEnrollmentCourseDetails
import soaringLizards.blps.dto.edx.EdxEnrollmentRequest
import soaringLizards.blps.dto.edx.EdxEnrollmentResponse
import soaringLizards.blps.dto.user.Email
import java.time.Instant
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class EdxServiceTest {
    private val edxCoursesClient = mock<EdxCoursesClient>()
    private val edxEnrollmentClient = mock<EdxEnrollmentClient>()
    private val edxProgressClient = mock<EdxProgressClient>()

    private val service = EdxService(
        edxCoursesClient = edxCoursesClient,
        edxEnrollmentClient = edxEnrollmentClient,
        edxProgressClient = edxProgressClient,
    )

    @Test
    fun `enroll maps top-level courseId into course_details for Open edX API`() {
        val user = testUser()
        val request = EdxEnrollmentRequest(
            courseId = "course-v1:OpenedX+DemoX+DemoCourse",
            mode = "audit",
            user = "admin",
        )
        whenever(edxEnrollmentClient.enrollCurrentUser(any(), any()))
            .thenReturn(EdxEnrollmentResponse())

        service.enroll(user, request)

        val captor = argumentCaptor<EdxEnrollmentRequest>()
        verify(edxEnrollmentClient).enrollCurrentUser(eq(user.edxSession!!), captor.capture())
        assertEquals("course-v1:OpenedX+DemoX+DemoCourse", captor.firstValue.courseDetails?.courseId)
        assertEquals("audit", captor.firstValue.mode)
        assertEquals("admin", captor.firstValue.user)
    }

    @Test
    fun `enroll keeps explicit course_details untouched`() {
        val user = testUser()
        val request = EdxEnrollmentRequest(
            courseDetails = EdxEnrollmentCourseDetails("course-v1:OpenedX+DemoX+DemoCourse"),
            courseId = "course-v1:another+id",
            mode = "audit",
            user = "admin",
        )
        whenever(edxEnrollmentClient.enrollCurrentUser(any(), any()))
            .thenReturn(EdxEnrollmentResponse())

        service.enroll(user, request)

        val captor = argumentCaptor<EdxEnrollmentRequest>()
        verify(edxEnrollmentClient).enrollCurrentUser(eq(user.edxSession!!), captor.capture())
        assertNotNull(captor.firstValue.courseDetails)
        assertEquals("course-v1:OpenedX+DemoX+DemoCourse", captor.firstValue.courseDetails!!.courseId)
    }

    private fun testUser() =
        JwtUserDetails(
            id = UUID.randomUUID(),
            _username = "admin",
            email = Email("admin@example.com"),
            role = "OPENEDX_USER",
            token = "token",
            edxSession = EdxAuthenticatedSession(
                cookieHeader = "sessionid=test",
                csrfToken = "csrf",
                expiresAt = Instant.now().plusSeconds(3600),
            ),
        )
}
