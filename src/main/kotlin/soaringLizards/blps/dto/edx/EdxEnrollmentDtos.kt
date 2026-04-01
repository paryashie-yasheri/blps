package soaringLizards.blps.dto.edx

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import java.time.OffsetDateTime

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
data class EdxEnrollmentRequest(
    @get:JsonProperty("course_details")
    val courseDetails: EdxEnrollmentCourseDetails? = null,
    @get:JsonProperty("course_id")
    val courseId: String? = null,
    @get:JsonProperty("mode")
    val mode: String? = null,
    @get:JsonProperty("is_active")
    val isActive: Boolean? = null,
    @get:JsonProperty("email_opt_in")
    val emailOptIn: Boolean? = null,
    @get:JsonProperty("force")
    val force: Boolean? = null,
    @get:JsonProperty("user")
    val user: String? = null,
    @get:JsonProperty("enterprise_course_consent")
    val enterpriseCourseConsent: Boolean? = null,
    @get:JsonProperty("enrollment_attributes")
    val enrollmentAttributes: Map<String, String>? = null,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
data class EdxEnrollmentCourseDetails(
    @get:JsonProperty("course_id")
    val courseId: String,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class EdxEnrollmentResponse(
    @get:JsonProperty("created")
    val created: OffsetDateTime? = null,
    @get:JsonProperty("mode")
    val mode: String? = null,
    @get:JsonProperty("is_active")
    val isActive: Boolean? = null,
    @get:JsonProperty("user")
    val user: String? = null,
    @get:JsonProperty("course_id")
    val courseId: String? = null,
    @get:JsonProperty("course_details")
    val courseDetails: EdxEnrollmentCourseResponseDetails? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class EdxEnrollmentCourseResponseDetails(
    @get:JsonProperty("course_id")
    val courseId: String? = null,
    @get:JsonProperty("course_name")
    val courseName: String? = null,
    @get:JsonProperty("enrollment_start")
    val enrollmentStart: OffsetDateTime? = null,
    @get:JsonProperty("enrollment_end")
    val enrollmentEnd: OffsetDateTime? = null,
    @get:JsonProperty("course_start")
    val courseStart: OffsetDateTime? = null,
    @get:JsonProperty("course_end")
    val courseEnd: OffsetDateTime? = null,
    @get:JsonProperty("invite_only")
    val inviteOnly: Boolean? = null,
    @get:JsonProperty("course_modes")
    val courseModes: List<EdxEnrollmentCourseMode> = emptyList(),
    @get:JsonProperty("pacing_type")
    val pacingType: String? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class EdxEnrollmentCourseMode(
    @get:JsonProperty("slug")
    val slug: String? = null,
    @get:JsonProperty("name")
    val name: String? = null,
    @get:JsonProperty("min_price")
    val minPrice: Int? = null,
    @get:JsonProperty("currency")
    val currency: String? = null,
)
