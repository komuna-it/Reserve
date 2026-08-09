package site.komuna.reserve.common.error

import java.time.OffsetDateTime

data class ErrorResponse(
    var errorType: ErrorType? = null,
    var message: String? = null,
    var bannedUntil: OffsetDateTime? = null
)