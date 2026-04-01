package soaringLizards.blps.dto.edx

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import java.time.OffsetDateTime

@JsonIgnoreProperties(ignoreUnknown = true)
data class EdxCoursesResponse(
    @get:JsonProperty("results")
    val results: List<EdxCourse> = emptyList(),
    @get:JsonProperty("pagination")
    val pagination: EdxCoursesPagination? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class EdxCoursesPagination(
    @get:JsonProperty("next")
    val next: String? = null,
    @get:JsonProperty("previous")
    val previous: String? = null,
    @get:JsonProperty("count")
    val count: Int? = null,
    @get:JsonProperty("num_pages")
    val numPages: Int? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class EdxCourse(
    @get:JsonProperty("id")
    val id: String,
    @get:JsonProperty("name")
    val name: String,
    @get:JsonProperty("number")
    val number: String? = null,
    @get:JsonProperty("org")
    val org: String? = null,
    @get:JsonProperty("short_description")
    val shortDescription: String? = null,
    @get:JsonProperty("start")
    val start: OffsetDateTime? = null,
    @get:JsonProperty("start_display")
    val startDisplay: String? = null,
    @get:JsonProperty("start_type")
    val startType: String? = null,
    @get:JsonProperty("pacing")
    val pacing: String? = null,
    @get:JsonProperty("mobile_available")
    val mobileAvailable: Boolean? = null,
    @get:JsonProperty("invitation_only")
    val invitationOnly: Boolean? = null,
    @get:JsonProperty("blocks_url")
    val blocksUrl: String? = null,
    @get:JsonProperty("effort")
    val effort: String? = null,
    @get:JsonProperty("end")
    val end: OffsetDateTime? = null,
    @get:JsonProperty("enrollment_start")
    val enrollmentStart: OffsetDateTime? = null,
    @get:JsonProperty("enrollment_end")
    val enrollmentEnd: OffsetDateTime? = null,
    @get:JsonProperty("hidden")
    val hidden: Boolean? = null,
    @get:JsonProperty("course_id")
    val courseId: String? = null,
    @get:JsonProperty("overview")
    val overview: String? = null,
    @get:JsonProperty("media")
    val media: EdxCourseMedia? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class EdxCourseMedia(
    @get:JsonProperty("banner_image")
    val bannerImage: EdxCourseAsset? = null,
    @get:JsonProperty("course_image")
    val courseImage: EdxCourseAsset? = null,
    @get:JsonProperty("course_video")
    val courseVideo: EdxCourseVideo? = null,
    @get:JsonProperty("image")
    val image: EdxCourseImageSet? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class EdxCourseAsset(
    @get:JsonProperty("uri")
    val uri: String? = null,
    @get:JsonProperty("uri_absolute")
    val uriAbsolute: String? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class EdxCourseVideo(
    @get:JsonProperty("uri")
    val uri: String? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class EdxCourseImageSet(
    @get:JsonProperty("raw")
    val raw: String? = null,
    @get:JsonProperty("small")
    val small: String? = null,
    @get:JsonProperty("large")
    val large: String? = null,
)
