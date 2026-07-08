package site.komuna.reserve.security.token.access

import java.time.OffsetDateTime

class AccessToken(
    val token: String,
    val expires: OffsetDateTime
) {
}