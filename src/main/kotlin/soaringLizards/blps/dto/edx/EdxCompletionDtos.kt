package soaringLizards.blps.dto.edx

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
data class EdxCompletionBatchRequest(
    @get:JsonProperty("username")
    val username: String,
    @get:JsonProperty("course_key")
    val courseKey: String,
    @get:JsonProperty("blocks")
    val blocks: Map<String, Double>,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class EdxCompletionBatchResponse(
    @get:JsonProperty("detail")
    val detail: String? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class EdxSubsectionCompletionResponse(
    @get:JsonProperty("completion")
    val completion: Double? = null,
)
