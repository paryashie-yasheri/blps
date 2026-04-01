package soaringLizards.blps.dto.edx

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import java.time.OffsetDateTime

@JsonIgnoreProperties(ignoreUnknown = true)
data class EdxCourseProgressResponse(
    @get:JsonProperty("verified_mode")
    val verifiedMode: String? = null,
    @get:JsonProperty("access_expiration")
    val accessExpiration: Map<String, Any?>? = null,
    @get:JsonProperty("certificate_data")
    val certificateData: Map<String, Any?>? = null,
    @get:JsonProperty("completion_summary")
    val completionSummary: Map<String, Any?>? = null,
    @get:JsonProperty("course_grade")
    val courseGrade: Map<String, Any?>? = null,
    @get:JsonProperty("credit_course_requirements")
    val creditCourseRequirements: Map<String, Any?>? = null,
    @get:JsonProperty("end")
    val end: OffsetDateTime? = null,
    @get:JsonProperty("enrollment_mode")
    val enrollmentMode: String? = null,
    @get:JsonProperty("grading_policy")
    val gradingPolicy: Map<String, Any?>? = null,
    @get:JsonProperty("has_scheduled_content")
    val hasScheduledContent: Boolean? = null,
    @get:JsonProperty("section_scores")
    val sectionScores: List<Map<String, Any?>> = emptyList(),
    @get:JsonProperty("studio_url")
    val studioUrl: String? = null,
    @get:JsonProperty("username")
    val username: String? = null,
    @get:JsonProperty("user_has_passing_grade")
    val userHasPassingGrade: Boolean? = null,
    @get:JsonProperty("verification_data")
    val verificationData: Map<String, Any?>? = null,
    @get:JsonProperty("disable_progress_graph")
    val disableProgressGraph: Boolean? = null,
    @get:JsonProperty("assignment_type_grade_summary")
    val assignmentTypeGradeSummary: List<Map<String, Any?>> = emptyList(),
    @get:JsonProperty("final_grades")
    val finalGrades: Double? = null,
)
