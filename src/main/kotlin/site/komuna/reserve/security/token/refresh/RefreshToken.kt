package site.komuna.reserve.security.token.refresh

import java.time.OffsetDateTime

class RefreshToken(
    val token: String,
    val expires: OffsetDateTime
) {

    constructor(token: RefreshTokenEntity) : this(
        token.token,
        token.expires
    )

}