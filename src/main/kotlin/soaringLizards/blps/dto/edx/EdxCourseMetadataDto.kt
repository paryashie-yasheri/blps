package soaringLizards.blps.dto.edx

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import java.time.OffsetDateTime

@JsonIgnoreProperties(ignoreUnknown = true)
data class EdxCourseMetadataResponse(
    @get:JsonProperty("verified_mode")
    val verifiedMode: String? = null,
    @get:JsonProperty("celebrations")
    val celebrations: Map<String, Any?>? = null,
    @get:JsonProperty("course_access")
    val courseAccess: EdxCourseAccess? = null,
    @get:JsonProperty("studio_access")
    val studioAccess: Boolean? = null,
    @get:JsonProperty("course_id")
    val courseId: String,
    @get:JsonProperty("is_enrolled")
    val isEnrolled: Boolean? = null,
    @get:JsonProperty("is_self_paced")
    val isSelfPaced: Boolean? = null,
    @get:JsonProperty("is_staff")
    val isStaff: Boolean? = null,
    @get:JsonProperty("number")
    val number: String? = null,
    @get:JsonProperty("org")
    val org: String? = null,
    @get:JsonProperty("original_user_is_staff")
    val originalUserIsStaff: Boolean? = null,
    @get:JsonProperty("start")
    val start: OffsetDateTime? = null,
    @get:JsonProperty("tabs")
    val tabs: List<EdxCourseTab> = emptyList(),
    @get:JsonProperty("title")
    val title: String? = null,
    @get:JsonProperty("username")
    val username: String? = null,
    @get:JsonProperty("user_timezone")
    val userTimezone: String? = null,
    @get:JsonProperty("can_view_certificate")
    val canViewCertificate: Boolean? = null,
    @get:JsonProperty("course_modes")
    val courseModes: List<EdxCourseMode> = emptyList(),
    @get:JsonProperty("is_new_discussion_sidebar_view_enabled")
    val isNewDiscussionSidebarViewEnabled: Boolean? = null,
    @get:JsonProperty("has_course_author_access")
    val hasCourseAuthorAccess: Boolean? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class EdxCourseAccess(
    @get:JsonProperty("has_access")
    val hasAccess: Boolean? = null,
    @get:JsonProperty("error_code")
    val errorCode: String? = null,
    @get:JsonProperty("developer_message")
    val developerMessage: String? = null,
    @get:JsonProperty("user_message")
    val userMessage: String? = null,
    @get:JsonProperty("additional_context_user_message")
    val additionalContextUserMessage: String? = null,
    @get:JsonProperty("user_fragment")
    val userFragment: String? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class EdxCourseTab(
    @get:JsonProperty("tab_id")
    val tabId: String,
    @get:JsonProperty("title")
    val title: String? = null,
    @get:JsonProperty("url")
    val url: String? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class EdxCourseMode(
    @get:JsonProperty("slug")
    val slug: String,
    @get:JsonProperty("name")
    val name: String,
)
