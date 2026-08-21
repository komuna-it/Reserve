package site.komuna.reserve.common.httpError

import java.time.OffsetDateTime
import java.time.ZoneOffset

/**
 * ReserveErrorResponse is a standardized error response format.
 */
data class ReserveErrorResponse(
    val timestamp: OffsetDateTime = OffsetDateTime.now(ZoneOffset.UTC),
    val type: String,
    val body: Any?
)