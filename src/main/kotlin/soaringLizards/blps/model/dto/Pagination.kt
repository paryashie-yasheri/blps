package soaringLizards.blps.model.dto

import io.swagger.v3.oas.annotations.media.Schema
import kotlin.math.max
import kotlin.math.min

@Schema(description = "Pagination request")
data class PageRequest(
    @field:Schema(description = "Zero-based page index", example = "0")
    val page: Int = 0,
    @field:Schema(description = "Page size (max 100)", example = "20")
    val size: Int = 20,
) {
    private val safePage = max(0, page)
    private val safeSize = min(100, max(1, size))

    val limit: Int get() = safeSize
    val offset: Int get() = safePage * safeSize
    val normalizedPage: Int get() = safePage
    val normalizedSize: Int get() = safeSize
}

@Schema(description = "Paged response wrapper")
data class PagedResponse<T>(
    @field:Schema(description = "Zero-based page index", example = "0")
    val page: Int,
    @field:Schema(description = "Page size", example = "20")
    val size: Int,
    @field:Schema(description = "Total elements available", example = "120")
    val total: Long,
    @field:Schema(description = "Items in this page")
    val items: List<T>,
)


data class PageResult<T>(
    val items: List<T>,
    val total: Long,
)