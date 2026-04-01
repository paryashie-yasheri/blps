package soaringLizards.blps.dto.edx

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import java.time.OffsetDateTime

@JsonIgnoreProperties(ignoreUnknown = true)
data class EdxCourseOutlineResponse(
    @get:JsonProperty("dates_banner_info")
    val datesBannerInfo: Map<String, Any?>? = null,
    @get:JsonProperty("verified_mode")
    val verifiedMode: String? = null,
    @get:JsonProperty("access_expiration")
    val accessExpiration: Map<String, Any?>? = null,
    @get:JsonProperty("cert_data")
    val certData: Map<String, Any?>? = null,
    @get:JsonProperty("course_blocks")
    val courseBlocks: Map<String, Any?>? = null,
    @get:JsonProperty("course_goals")
    val courseGoals: EdxCourseGoals? = null,
    @get:JsonProperty("course_tools")
    val courseTools: List<EdxCourseTool> = emptyList(),
    @get:JsonProperty("dates_widget")
    val datesWidget: EdxDatesWidget? = null,
    @get:JsonProperty("enroll_alert")
    val enrollAlert: EdxEnrollAlert? = null,
    @get:JsonProperty("enrollment_mode")
    val enrollmentMode: String? = null,
    @get:JsonProperty("enable_proctored_exams")
    val enableProctoredExams: Boolean? = null,
    @get:JsonProperty("handouts_html")
    val handoutsHtml: String? = null,
    @get:JsonProperty("has_ended")
    val hasEnded: Boolean? = null,
    @get:JsonProperty("offer")
    val offer: Map<String, Any?>? = null,
    @get:JsonProperty("resume_course")
    val resumeCourse: EdxResumeCourse? = null,
    @get:JsonProperty("welcome_message_html")
    val welcomeMessageHtml: String? = null,
    @get:JsonProperty("user_has_passing_grade")
    val userHasPassingGrade: Boolean? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class EdxCourseGoals(
    @get:JsonProperty("selected_goal")
    val selectedGoal: EdxSelectedGoal? = null,
    @get:JsonProperty("weekly_learning_goal_enabled")
    val weeklyLearningGoalEnabled: Boolean? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class EdxSelectedGoal(
    @get:JsonProperty("days_per_week")
    val daysPerWeek: Int? = null,
    @get:JsonProperty("subscribed_to_reminders")
    val subscribedToReminders: Boolean? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class EdxCourseTool(
    @get:JsonProperty("analytics_id")
    val analyticsId: String? = null,
    @get:JsonProperty("title")
    val title: String? = null,
    @get:JsonProperty("url")
    val url: String? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class EdxDatesWidget(
    @get:JsonProperty("course_date_blocks")
    val courseDateBlocks: List<EdxCourseDateBlock> = emptyList(),
    @get:JsonProperty("dates_tab_link")
    val datesTabLink: String? = null,
    @get:JsonProperty("user_timezone")
    val userTimezone: String? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class EdxCourseDateBlock(
    @get:JsonProperty("complete")
    val complete: Boolean? = null,
    @get:JsonProperty("date")
    val date: OffsetDateTime? = null,
    @get:JsonProperty("date_type")
    val dateType: String? = null,
    @get:JsonProperty("description")
    val description: String? = null,
    @get:JsonProperty("learner_has_access")
    val learnerHasAccess: Boolean? = null,
    @get:JsonProperty("link")
    val link: String? = null,
    @get:JsonProperty("title")
    val title: String? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class EdxEnrollAlert(
    @get:JsonProperty("can_enroll")
    val canEnroll: Boolean? = null,
    @get:JsonProperty("extra_text")
    val extraText: String? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class EdxResumeCourse(
    @get:JsonProperty("has_visited_course")
    val hasVisitedCourse: Boolean? = null,
    @get:JsonProperty("url")
    val url: String? = null,
)
