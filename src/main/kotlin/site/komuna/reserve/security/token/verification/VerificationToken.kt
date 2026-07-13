package site.komuna.reserve.security.token.verification

import java.time.OffsetDateTime

class VerificationToken(
    val token: String,
    val expires: OffsetDateTime
) {

    constructor(tokenEntity: VerificationTokenEntity) : this(
        tokenEntity.token,
        tokenEntity.expires
    )
}